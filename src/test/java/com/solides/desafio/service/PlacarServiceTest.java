package com.solides.desafio.service;

import com.solides.desafio.infra.rabbitmq.PlacarProducer;
import com.solides.desafio.infra.redis.RedisClientProvider;
import com.solides.desafio.repository.PlacarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlacarService covering main branches and error handling.
 */
@ExtendWith(MockitoExtension.class)
class PlacarServiceTest {

    @Mock
    PlacarRepository placarRepository;

    @Mock
    PlacarProducer producer;

    @Mock
    RedisClientProvider redisProvider;

    @Mock
    Jedis jedis;

    @InjectMocks
    PlacarService service;

    private final String initialPayload = "{\"time_da_casa\":{\"nome\":\"A\",\"pontos\":0},\"time_visitante\":{\"nome\":\"B\",\"pontos\":0}}";
    private final String initialResponse = "{\"hash_id\":\"abc123\"}";

    @BeforeEach
    void setup() {
        when(redisProvider.getJedis()).thenReturn(jedis);
    }


    @Test
    void iniciar_shouldCallRepositoryAndStoreInRedis_whenProcedureReturnsHash() throws Exception {
        when(placarRepository.iniciar(initialPayload)).thenReturn(initialResponse);

        String res = service.iniciar(initialPayload);

        assertEquals(initialResponse, res);
        byte[] expectedKey = "placar:abc123".getBytes(StandardCharsets.UTF_8);
        byte[] expectedValue = initialPayload.getBytes(StandardCharsets.UTF_8);
        verify(jedis, times(1)).set(eq(expectedKey), eq(expectedValue));
        verify(placarRepository, times(1)).iniciar(initialPayload);
    }

    @Test
    void iniciar_shouldNotPropagate_whenRedisSetThrows() throws Exception {
        when(placarRepository.iniciar(initialPayload)).thenReturn(initialResponse);
        doThrow(new RuntimeException("redis down")).when(jedis).set((byte[]) any(), any());

        String res = service.iniciar(initialPayload);

        assertEquals(initialResponse, res);
        verify(placarRepository, times(1)).iniciar(initialPayload);
        verify(jedis, times(1)).set((byte[]) any(), any());
    }


    @Test
    void pontuar_shouldFallbackToDb_whenRedisDoesNotHaveValue() throws Exception {
        String hash = "abc123";
        when(jedis.get(("placar:" + hash).getBytes(StandardCharsets.UTF_8))).thenReturn(null);

        when(placarRepository.buscarDadosPorHash(hash)).thenReturn(Optional.of(initialPayload));

        String updatedJson = "{\"time_da_casa\":{\"pontos\":1},\"time_visitante\":{\"pontos\":0}}";
        when(placarRepository.atualizar(eq(hash), anyString())).thenReturn(updatedJson);

        String result = service.pontuar(hash, "casa");

        assertEquals(updatedJson, result);
        verify(placarRepository, times(1)).buscarDadosPorHash(hash);
        verify(placarRepository, times(1)).atualizar(eq(hash), anyString());
        verify(producer, times(1)).enviarEvento(anyString());
        verify(jedis, times(1)).set(eq(("placar:" + hash).getBytes(StandardCharsets.UTF_8)),
                eq(updatedJson.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void pontuar_shouldThrowIllegalArgument_whenNotFoundInDbAndRedis() {
        String hash = "noexist";
        when(jedis.get(("placar:" + hash).getBytes(StandardCharsets.UTF_8))).thenReturn(null);
        when(placarRepository.buscarDadosPorHash(hash)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.pontuar(hash, "casa"));
        assertTrue(ex.getMessage().contains("Placar não encontrado"));
        verify(placarRepository, times(1)).buscarDadosPorHash(hash);
    }

    @Test
    void pontuar_shouldThrowIllegalArgument_whenLadoInvalid() {
        String hash = "abc123";
        when(jedis.get(("placar:" + hash).getBytes(StandardCharsets.UTF_8)))
                .thenReturn(initialPayload.getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.pontuar(hash, "meio"));
        assertTrue(ex.getMessage().contains("lado inválido"));
        verify(placarRepository, never()).atualizar(anyString(), anyString());
    }

    @Test
    void pontuar_shouldContinue_whenProducerThrowsException() throws Exception {
        String hash = "abc123";
        when(jedis.get(("placar:" + hash).getBytes(StandardCharsets.UTF_8)))
                .thenReturn(initialPayload.getBytes(StandardCharsets.UTF_8));

        String updatedJson = "{\"time_da_casa\":{\"pontos\":1},\"time_visitante\":{\"pontos\":0}}";
        when(placarRepository.atualizar(eq(hash), anyString())).thenReturn(updatedJson);

        doThrow(new RuntimeException("rabbit failed")).when(producer).enviarEvento(anyString());

        String res = service.pontuar(hash, "casa");

        assertEquals(updatedJson, res);
        verify(producer, times(1)).enviarEvento(anyString());
        verify(jedis, times(1)).set(eq(("placar:" + hash).getBytes(StandardCharsets.UTF_8)),
                eq(updatedJson.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void pontuar_shouldNotPropagate_whenRedisSetThrowsAfterUpdate() throws Exception {
        String hash = "abc123";
        when(jedis.get(("placar:" + hash).getBytes(StandardCharsets.UTF_8)))
                .thenReturn(initialPayload.getBytes(StandardCharsets.UTF_8));

        String updatedJson = "{\"time_da_casa\":{\"pontos\":1},\"time_visitante\":{\"pontos\":0}}";
        when(placarRepository.atualizar(eq(hash), anyString())).thenReturn(updatedJson);

        doThrow(new RuntimeException("redis set fail")).when(jedis)
                .set(eq(("placar:" + hash).getBytes(StandardCharsets.UTF_8)), eq(updatedJson.getBytes(StandardCharsets.UTF_8)));

        String res = service.pontuar(hash, "casa");

        assertEquals(updatedJson, res);
        verify(jedis, times(1)).set(eq(("placar:" + hash).getBytes(StandardCharsets.UTF_8)), any(byte[].class));
    }

    // ---------- buscar ----------

    @Test
    void buscar_shouldReturnFromRedis_whenExists() {
        String hash = "abc123";
        when(jedis.get(("placar:" + hash).getBytes(StandardCharsets.UTF_8)))
                .thenReturn(initialPayload.getBytes(StandardCharsets.UTF_8));

        Optional<String> opt = service.buscar(hash);
        assertTrue(opt.isPresent());
        assertEquals(initialPayload, opt.get());
        verify(redisProvider, times(1)).getJedis();
    }

    @Test
    void buscar_shouldFallbackToDb_whenRedisThrows() {
        String hash = "abc123";
        when(redisProvider.getJedis()).thenReturn(jedis);
        when(jedis.get((byte[]) any())).thenThrow(new RuntimeException("redis fail"));

        when(placarRepository.buscarDadosPorHash(hash)).thenReturn(Optional.of(initialPayload));

        Optional<String> opt = service.buscar(hash);
        assertTrue(opt.isPresent());
        assertEquals(initialPayload, opt.get());
        verify(placarRepository, times(1)).buscarDadosPorHash(hash);
    }

    @Test
    void buscar_shouldReturnEmpty_whenNotFoundAnywhere() {
        String hash = "none";
        when(jedis.get(("placar:" + hash).getBytes(StandardCharsets.UTF_8))).thenReturn(null);
        when(placarRepository.buscarDadosPorHash(hash)).thenReturn(Optional.empty());

        Optional<String> opt = service.buscar(hash);
        assertTrue(opt.isEmpty());
        verify(placarRepository, times(1)).buscarDadosPorHash(hash);
    }


    @Test
    void finalizar_shouldCallRepoAndDeleteRedis() {
        String hash = "abc123";

        doNothing().when(placarRepository).finalizar(hash);

        service.finalizar(hash);

        verify(placarRepository, times(1)).finalizar(hash);
        verify(jedis, times(1)).del(eq(("placar:" + hash).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void finalizar_shouldNotPropagate_whenRedisDelThrows() {
        String hash = "abc123";
        doNothing().when(placarRepository).finalizar(hash);
        doThrow(new RuntimeException("redis del fail")).when(jedis).del((byte[]) any());

        service.finalizar(hash);

        verify(placarRepository, times(1)).finalizar(hash);
        verify(jedis, times(1)).del(eq(("placar:" + hash).getBytes(StandardCharsets.UTF_8)));
    }
}
