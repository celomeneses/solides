FROM payara/micro:6.2024
COPY target/desafio-solides.war $PAYARA_PATH/deployments/
EXPOSE 8080