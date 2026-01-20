# produto-crud-reativo
Aplicação Java 21, Gradle, Quarkus reativo e integração com kafka, postgres 

# how to run

- first, clone repository
- and them, check your java version, and put it to grallvm 21
- also, run docker compose to create server: zookeeper, kfka-produto, postgres-produto
- build and run with ./gradlew clean build
- and finish in your command line into your IDE ./gradlew quarkusDev or start

# This app have the follows endpoints: curls to postman

 - just msg: (POST)
curl --location 'localhost:8080/produto/message' \ 
    --header 'Content-Type: application/json' \ 
    --data '"arroz"'
 
 - msg as product: (POST)
curl --location 'localhost:8080/produto/message/produto' \
    --header 'Content-Type: application/json' \
    --data '{
    "id":"1",
    "nome":"carne"
    }'

 - create a product: (POST)
curl --location 'localhost:8080/produto/create' \
    --header 'Content-Type: application/json' \
    --data '{    
    "nome":"Spaghett Adria n7"
    }'

 - findAll product: (GET)
curl --location 'localhost:8080/produto'

 - findbyId product: (GET)
curl --location 'localhost:8080/produto/20'

 - update product: (PATCH)
curl --location --request PATCH 'localhost:8080/produto/21' \
  --header 'Content-Type: application/json' \
  --data '{
  "nome":""
  }'

 - delete product: (DELETE)
curl --location --request DELETE 'localhost:8080/produto/remove/13'


# conclusion

I writhed this application for trainee and upgrade my skills.
In my opnian about use this tech, i recommend, it´s so much easy to integrate and make work all.
simple and fast kafka work around.