package com.onlinestore.tests;

import com.onlinestore.contracts.CatalogSeed;
import com.onlinestore.grpc.inventory.InventoryGrpc;
import com.onlinestore.grpc.inventory.InventoryOperationRequest;
import com.onlinestore.inventoryservice.InventoryServiceApplication;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InventoryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "grpc.server.port=19091")
class InventoryGrpcIT {

    private ManagedChannel channel;
    private InventoryGrpc.InventoryBlockingStub stub;

    @BeforeEach
    void setUp() {
        channel = ManagedChannelBuilder.forAddress("localhost", 19091).usePlaintext().build();
        stub = InventoryGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    void reserveViaGrpc_succeedsForSeededProduct() {
        var productId = CatalogSeed.defaultProducts().getFirst().productId().toString();
        var reply = stub.reserve(InventoryOperationRequest.newBuilder()
                .setProductId(productId)
                .setQuantity(1)
                .build());

        assertThat(reply.getSuccess()).isTrue();
        assertThat(reply.getStatusCode()).isEqualTo(200);
    }

    @Test
    void checkViaGrpc_failsWhenQuantityExceedsStock() {
        var productId = CatalogSeed.defaultProducts().getFirst().productId().toString();
        var reply = stub.check(InventoryOperationRequest.newBuilder()
                .setProductId(productId)
                .setQuantity(10_000)
                .build());

        assertThat(reply.getSuccess()).isFalse();
        assertThat(reply.getStatusCode()).isEqualTo(400);
    }
}
