package com.workflow.ulid.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * ULID service definition
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.73.0)",
    comments = "Source: ulid.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class UlidServiceGrpc {

  private UlidServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "ulid.UlidService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.workflow.ulid.grpc.UlidRequest,
      com.workflow.ulid.grpc.UlidResponse> getGenerateUlidMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GenerateUlid",
      requestType = com.workflow.ulid.grpc.UlidRequest.class,
      responseType = com.workflow.ulid.grpc.UlidResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.workflow.ulid.grpc.UlidRequest,
      com.workflow.ulid.grpc.UlidResponse> getGenerateUlidMethod() {
    io.grpc.MethodDescriptor<com.workflow.ulid.grpc.UlidRequest, com.workflow.ulid.grpc.UlidResponse> getGenerateUlidMethod;
    if ((getGenerateUlidMethod = UlidServiceGrpc.getGenerateUlidMethod) == null) {
      synchronized (UlidServiceGrpc.class) {
        if ((getGenerateUlidMethod = UlidServiceGrpc.getGenerateUlidMethod) == null) {
          UlidServiceGrpc.getGenerateUlidMethod = getGenerateUlidMethod =
              io.grpc.MethodDescriptor.<com.workflow.ulid.grpc.UlidRequest, com.workflow.ulid.grpc.UlidResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GenerateUlid"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.workflow.ulid.grpc.UlidRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.workflow.ulid.grpc.UlidResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UlidServiceMethodDescriptorSupplier("GenerateUlid"))
              .build();
        }
      }
    }
    return getGenerateUlidMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.workflow.ulid.grpc.UlidRequest,
      com.workflow.ulid.grpc.UlidResponse> getGenerateMonotonicUlidMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GenerateMonotonicUlid",
      requestType = com.workflow.ulid.grpc.UlidRequest.class,
      responseType = com.workflow.ulid.grpc.UlidResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.workflow.ulid.grpc.UlidRequest,
      com.workflow.ulid.grpc.UlidResponse> getGenerateMonotonicUlidMethod() {
    io.grpc.MethodDescriptor<com.workflow.ulid.grpc.UlidRequest, com.workflow.ulid.grpc.UlidResponse> getGenerateMonotonicUlidMethod;
    if ((getGenerateMonotonicUlidMethod = UlidServiceGrpc.getGenerateMonotonicUlidMethod) == null) {
      synchronized (UlidServiceGrpc.class) {
        if ((getGenerateMonotonicUlidMethod = UlidServiceGrpc.getGenerateMonotonicUlidMethod) == null) {
          UlidServiceGrpc.getGenerateMonotonicUlidMethod = getGenerateMonotonicUlidMethod =
              io.grpc.MethodDescriptor.<com.workflow.ulid.grpc.UlidRequest, com.workflow.ulid.grpc.UlidResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GenerateMonotonicUlid"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.workflow.ulid.grpc.UlidRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.workflow.ulid.grpc.UlidResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UlidServiceMethodDescriptorSupplier("GenerateMonotonicUlid"))
              .build();
        }
      }
    }
    return getGenerateMonotonicUlidMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.workflow.ulid.grpc.BatchRequest,
      com.workflow.ulid.grpc.BatchResponse> getReserveBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReserveBatch",
      requestType = com.workflow.ulid.grpc.BatchRequest.class,
      responseType = com.workflow.ulid.grpc.BatchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.workflow.ulid.grpc.BatchRequest,
      com.workflow.ulid.grpc.BatchResponse> getReserveBatchMethod() {
    io.grpc.MethodDescriptor<com.workflow.ulid.grpc.BatchRequest, com.workflow.ulid.grpc.BatchResponse> getReserveBatchMethod;
    if ((getReserveBatchMethod = UlidServiceGrpc.getReserveBatchMethod) == null) {
      synchronized (UlidServiceGrpc.class) {
        if ((getReserveBatchMethod = UlidServiceGrpc.getReserveBatchMethod) == null) {
          UlidServiceGrpc.getReserveBatchMethod = getReserveBatchMethod =
              io.grpc.MethodDescriptor.<com.workflow.ulid.grpc.BatchRequest, com.workflow.ulid.grpc.BatchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReserveBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.workflow.ulid.grpc.BatchRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.workflow.ulid.grpc.BatchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UlidServiceMethodDescriptorSupplier("ReserveBatch"))
              .build();
        }
      }
    }
    return getReserveBatchMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static UlidServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UlidServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UlidServiceStub>() {
        @java.lang.Override
        public UlidServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UlidServiceStub(channel, callOptions);
        }
      };
    return UlidServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static UlidServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UlidServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UlidServiceBlockingV2Stub>() {
        @java.lang.Override
        public UlidServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UlidServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return UlidServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static UlidServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UlidServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UlidServiceBlockingStub>() {
        @java.lang.Override
        public UlidServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UlidServiceBlockingStub(channel, callOptions);
        }
      };
    return UlidServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static UlidServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UlidServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UlidServiceFutureStub>() {
        @java.lang.Override
        public UlidServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UlidServiceFutureStub(channel, callOptions);
        }
      };
    return UlidServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * ULID service definition
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Generates a standard ULID
     * </pre>
     */
    default void generateUlid(com.workflow.ulid.grpc.UlidRequest request,
        io.grpc.stub.StreamObserver<com.workflow.ulid.grpc.UlidResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGenerateUlidMethod(), responseObserver);
    }

    /**
     * <pre>
     * Generates a monotonic ULID
     * </pre>
     */
    default void generateMonotonicUlid(com.workflow.ulid.grpc.UlidRequest request,
        io.grpc.stub.StreamObserver<com.workflow.ulid.grpc.UlidResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGenerateMonotonicUlidMethod(), responseObserver);
    }

    /**
     * <pre>
     * Reserves a batch of ULIDs for client-side generation
     * </pre>
     */
    default void reserveBatch(com.workflow.ulid.grpc.BatchRequest request,
        io.grpc.stub.StreamObserver<com.workflow.ulid.grpc.BatchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReserveBatchMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service UlidService.
   * <pre>
   * ULID service definition
   * </pre>
   */
  public static abstract class UlidServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return UlidServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service UlidService.
   * <pre>
   * ULID service definition
   * </pre>
   */
  public static final class UlidServiceStub
      extends io.grpc.stub.AbstractAsyncStub<UlidServiceStub> {
    private UlidServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UlidServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UlidServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Generates a standard ULID
     * </pre>
     */
    public void generateUlid(com.workflow.ulid.grpc.UlidRequest request,
        io.grpc.stub.StreamObserver<com.workflow.ulid.grpc.UlidResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGenerateUlidMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Generates a monotonic ULID
     * </pre>
     */
    public void generateMonotonicUlid(com.workflow.ulid.grpc.UlidRequest request,
        io.grpc.stub.StreamObserver<com.workflow.ulid.grpc.UlidResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGenerateMonotonicUlidMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Reserves a batch of ULIDs for client-side generation
     * </pre>
     */
    public void reserveBatch(com.workflow.ulid.grpc.BatchRequest request,
        io.grpc.stub.StreamObserver<com.workflow.ulid.grpc.BatchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReserveBatchMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service UlidService.
   * <pre>
   * ULID service definition
   * </pre>
   */
  public static final class UlidServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<UlidServiceBlockingV2Stub> {
    private UlidServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UlidServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UlidServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Generates a standard ULID
     * </pre>
     */
    public com.workflow.ulid.grpc.UlidResponse generateUlid(com.workflow.ulid.grpc.UlidRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateUlidMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Generates a monotonic ULID
     * </pre>
     */
    public com.workflow.ulid.grpc.UlidResponse generateMonotonicUlid(com.workflow.ulid.grpc.UlidRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateMonotonicUlidMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Reserves a batch of ULIDs for client-side generation
     * </pre>
     */
    public com.workflow.ulid.grpc.BatchResponse reserveBatch(com.workflow.ulid.grpc.BatchRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReserveBatchMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service UlidService.
   * <pre>
   * ULID service definition
   * </pre>
   */
  public static final class UlidServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<UlidServiceBlockingStub> {
    private UlidServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UlidServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UlidServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Generates a standard ULID
     * </pre>
     */
    public com.workflow.ulid.grpc.UlidResponse generateUlid(com.workflow.ulid.grpc.UlidRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateUlidMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Generates a monotonic ULID
     * </pre>
     */
    public com.workflow.ulid.grpc.UlidResponse generateMonotonicUlid(com.workflow.ulid.grpc.UlidRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateMonotonicUlidMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Reserves a batch of ULIDs for client-side generation
     * </pre>
     */
    public com.workflow.ulid.grpc.BatchResponse reserveBatch(com.workflow.ulid.grpc.BatchRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReserveBatchMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service UlidService.
   * <pre>
   * ULID service definition
   * </pre>
   */
  public static final class UlidServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<UlidServiceFutureStub> {
    private UlidServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UlidServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UlidServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Generates a standard ULID
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.workflow.ulid.grpc.UlidResponse> generateUlid(
        com.workflow.ulid.grpc.UlidRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGenerateUlidMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Generates a monotonic ULID
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.workflow.ulid.grpc.UlidResponse> generateMonotonicUlid(
        com.workflow.ulid.grpc.UlidRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGenerateMonotonicUlidMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Reserves a batch of ULIDs for client-side generation
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.workflow.ulid.grpc.BatchResponse> reserveBatch(
        com.workflow.ulid.grpc.BatchRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReserveBatchMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GENERATE_ULID = 0;
  private static final int METHODID_GENERATE_MONOTONIC_ULID = 1;
  private static final int METHODID_RESERVE_BATCH = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GENERATE_ULID:
          serviceImpl.generateUlid((com.workflow.ulid.grpc.UlidRequest) request,
              (io.grpc.stub.StreamObserver<com.workflow.ulid.grpc.UlidResponse>) responseObserver);
          break;
        case METHODID_GENERATE_MONOTONIC_ULID:
          serviceImpl.generateMonotonicUlid((com.workflow.ulid.grpc.UlidRequest) request,
              (io.grpc.stub.StreamObserver<com.workflow.ulid.grpc.UlidResponse>) responseObserver);
          break;
        case METHODID_RESERVE_BATCH:
          serviceImpl.reserveBatch((com.workflow.ulid.grpc.BatchRequest) request,
              (io.grpc.stub.StreamObserver<com.workflow.ulid.grpc.BatchResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGenerateUlidMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.workflow.ulid.grpc.UlidRequest,
              com.workflow.ulid.grpc.UlidResponse>(
                service, METHODID_GENERATE_ULID)))
        .addMethod(
          getGenerateMonotonicUlidMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.workflow.ulid.grpc.UlidRequest,
              com.workflow.ulid.grpc.UlidResponse>(
                service, METHODID_GENERATE_MONOTONIC_ULID)))
        .addMethod(
          getReserveBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.workflow.ulid.grpc.BatchRequest,
              com.workflow.ulid.grpc.BatchResponse>(
                service, METHODID_RESERVE_BATCH)))
        .build();
  }

  private static abstract class UlidServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    UlidServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.workflow.ulid.grpc.UlidServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("UlidService");
    }
  }

  private static final class UlidServiceFileDescriptorSupplier
      extends UlidServiceBaseDescriptorSupplier {
    UlidServiceFileDescriptorSupplier() {}
  }

  private static final class UlidServiceMethodDescriptorSupplier
      extends UlidServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    UlidServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (UlidServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new UlidServiceFileDescriptorSupplier())
              .addMethod(getGenerateUlidMethod())
              .addMethod(getGenerateMonotonicUlidMethod())
              .addMethod(getReserveBatchMethod())
              .build();
        }
      }
    }
    return result;
  }
}
