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

## Protocol specification

### Handshake

| size | format  | description
|:-----|:--------|:-----------
|4     |int32    |Protocol version
|8     |int64    |UNIX timestamp millis
|~     |varstring|Peer name

### GetPeers

| size | format  | description
|:-----|:--------|:-----------
|4     |int32    |Max desired number of peers

### Peers

| size | format            | description
|:-----|:------------------|:-----------
|~     |inetSocketAddress[]|Peers

### GetFacts

| size | format            | description
|:-----|:------------------|:-----------
|32    |bytes              |Request ID
|~     |varstring[]        |Tags
|4     |int32              |Request TTL
|8     |int64              |UNIX timestamp millis

### GetFacts

| size | format            | description
|:-----|:------------------|:-----------
|32    |bytes              |Request ID
|~     |fact[]             |Array of facts matching corresponding search request

### Notes

`varstring` - UTF-8 string prefixed with 16 bit unsigned integer encoding string length in bits.
`inetSocketAddress` - IPv4 or IPv6 address bytes prefixed with 8 bit unsigned integer encoding address length in bytes + port encoded as 32 bit signed integer.
`[]` - array of elements prefixed with 16 bit unsigned integer encoding number of elements in the array.
`fact` - `varstring` (fact) + `varstring[]` (array of tags)
