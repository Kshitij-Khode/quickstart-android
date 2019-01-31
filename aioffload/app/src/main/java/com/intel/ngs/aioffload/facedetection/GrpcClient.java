package com.intel.ngs.aioffload.facedetection;

import android.graphics.Bitmap;

import org.tensorflow.framework.TensorProto;
import org.tensorflow.framework.TensorShapeProto;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import tensorflow.serving.Model;
import tensorflow.serving.Predict;
import tensorflow.serving.PredictionServiceGrpc;

public class GrpcClient {

  private String host;
  private int port;

  private ManagedChannel channel;
  private PredictionServiceGrpc.PredictionServiceBlockingStub blockingStub;

  public GrpcClient (String host, int port) {
    this.host = host;
    this.port = port;

    blockingStub = PredictionServiceGrpc.newBlockingStub(
        ManagedChannelBuilder.forAddress(host, port).build());
  }

  public void sendRequest(Bitmap[] faces, int numFaces) {
    Model.ModelSpec modelSpec = Model.ModelSpec.newBuilder()
        .setName("facenet").setSignatureName("remote_inference").build();

    TensorProto.Builder tensorProtoBuilder = TensorProto.newBuilder();

    tensorProtoBuilder.setTensorShape(TensorShapeProto
        .newBuilder()
        .addDim(TensorShapeProto.Dim.newBuilder().setSize(numFaces).build())
        .build());

    for (Bitmap bmp: faces) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
      tensorProtoBuilder.addStringVal(com.google.protobuf.ByteString.copyFrom(stream.toByteArray()));
    }

    Predict.PredictRequest request = Predict.PredictRequest.newBuilder()
        .setModelSpec(modelSpec)
        .putInputs("data", tensorProtoBuilder.build())
        .build();

    try {
      Predict.PredictResponse response = blockingStub
          .withDeadlineAfter(10, TimeUnit.MILLISECONDS)
          .predict(request);
      //System.out.println(response);
    } catch (StatusRuntimeException e) {
      e.printStackTrace();
    }
  }
}

