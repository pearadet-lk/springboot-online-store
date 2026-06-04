package com.onlinestore.inventoryservice;

import com.onlinestore.grpc.inventory.InventoryGrpc;
import com.onlinestore.grpc.inventory.InventoryOperationReply;
import com.onlinestore.grpc.inventory.InventoryOperationRequest;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
public class InventoryGrpcEndpoint extends InventoryGrpc.InventoryImplBase {

    private final InventoryStore store;

    public InventoryGrpcEndpoint(InventoryStore store) {
        this.store = store;
    }

    @Override
    public void check(InventoryOperationRequest request, StreamObserver<InventoryOperationReply> responseObserver) {
        handle(
                request,
                responseObserver,
                (id, qty) -> InventoryOperations.tryCheck(store.items(), id, qty),
                "Insufficient stock.");
    }

    @Override
    public void reserve(InventoryOperationRequest request, StreamObserver<InventoryOperationReply> responseObserver) {
        handle(
                request,
                responseObserver,
                (id, qty) -> InventoryOperations.tryReserve(store.items(), id, qty),
                "Insufficient stock.");
    }

    @Override
    public void release(InventoryOperationRequest request, StreamObserver<InventoryOperationReply> responseObserver) {
        handle(
                request,
                responseObserver,
                (id, qty) -> InventoryOperations.tryRelease(store.items(), id, qty),
                "Cannot release more than reserved quantity.");
    }

    @Override
    public void commit(InventoryOperationRequest request, StreamObserver<InventoryOperationReply> responseObserver) {
        handle(
                request,
                responseObserver,
                (id, qty) -> InventoryOperations.tryCommit(store.items(), id, qty),
                "Cannot commit more than reserved quantity.");
    }

    private void handle(
            InventoryOperationRequest request,
            StreamObserver<InventoryOperationReply> responseObserver,
            Operation operation,
            String validationError) {
        UUID productId;
        try {
            productId = UUID.fromString(request.getProductId());
        } catch (IllegalArgumentException ex) {
            responseObserver.onNext(InventoryOperationReply.newBuilder()
                    .setSuccess(false)
                    .setStatusCode(400)
                    .setError("Invalid product id.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        var result = operation.apply(productId, request.getQuantity());
        responseObserver.onNext(toReply(result, validationError));
        responseObserver.onCompleted();
    }

    private InventoryOperationReply toReply(InventoryOperations.Result result, String validationError) {
        if (result.success()) {
            return InventoryOperationReply.newBuilder()
                    .setSuccess(true)
                    .setStatusCode(200)
                    .build();
        }
        return InventoryOperationReply.newBuilder()
                .setSuccess(false)
                .setStatusCode("not_found".equals(result.errorCode()) ? 404 : 400)
                .setError("not_found".equals(result.errorCode()) ? "Product not found." : validationError)
                .build();
    }

    @FunctionalInterface
    private interface Operation {
        InventoryOperations.Result apply(UUID productId, int quantity);
    }
}
