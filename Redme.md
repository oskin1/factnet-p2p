# Factnet

A peer-to-peer network for sharing facts. Search withing the network is based on message flooding.

## Run local test network

This requires Docker and docker-compose to be installed on your machine.

Type into your terminal:

`cd /project/dir`

`docker-compose build`

`docker-compose up -d`

Then check network status
`docker-compose ps`

An output should look like:
```
user@machine factnet-p2p % docker-compose ps                       
       Name                  Command           State                       Ports                     
-----------------------------------------------------------------------------------------------------
factnet-p2p_node1_1   java -jar /factnet.jar   Up      0.0.0.0:7081->7081/tcp, 0.0.0.0:9031->9031/tcp
factnet-p2p_node2_1   java -jar /factnet.jar   Up      0.0.0.0:7082->7082/tcp, 0.0.0.0:9032->9032/tcp
factnet-p2p_node3_1   java -jar /factnet.jar   Up      0.0.0.0:7083->7083/tcp, 0.0.0.0:9033->9033/tcp
```
