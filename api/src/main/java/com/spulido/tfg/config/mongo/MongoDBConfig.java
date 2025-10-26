package com.spulido.tfg.config.mongo;

import java.util.Collection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@EnableMongoRepositories(basePackages = "com.project")
public class MongoDBConfig extends AbstractMongoClientConfiguration {

    @Value("${mongodb.database.name}")
    private String databaseName;

    @Value("${mongodb.connection.uri}")
    private String connectionUri;

    @Override
    protected String getDatabaseName() {
        return this.databaseName;
    }

    @Override
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(buildConnectionString());
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }

    @Override
    public boolean autoIndexCreation() {
        return true;
    }

    @Override
    protected Collection<String> getMappingBasePackages() {
        return super.getMappingBasePackages();
    }

    private String buildConnectionString() {
        return String.format("%s/%s", connectionUri, databaseName);
    }
}
