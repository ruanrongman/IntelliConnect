package top.rslly.iot.transfer;

import com.alibaba.fastjson.JSON;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import top.rslly.iot.models.DataEntity;
import top.rslly.iot.services.DataServiceImpl;
import top.rslly.iot.services.ProductDataServiceImpl;
import top.rslly.iot.services.ProductDeviceServiceImpl;
import top.rslly.iot.utility.exhook.*;

import java.util.Arrays;
import java.util.UUID;

@GrpcService
public class HookProviderImpl extends HookProviderGrpc.HookProviderImplBase {

    @Autowired
    private ProductDeviceServiceImpl productDeviceService;
    @Autowired
    private DealThingsModel dealThingsModel;

    public void DEBUG(String fn, Object req) {
        System.out.printf(fn + ", request: " + req);
    }

    @Override
    public void onProviderLoaded(ProviderLoadedRequest request, StreamObserver<LoadedResponse> responseObserver) {
        // DEBUG("onProviderLoaded", request);
        HookSpec[] specs = {

                HookSpec.newBuilder().setName("client.connected").build(),
                HookSpec.newBuilder().setName("client.disconnected").build(),
                HookSpec.newBuilder().setName("message.delivered").build(),
                //HookSpec.newBuilder().setName("message.publish").build(),

        };
        LoadedResponse reply = LoadedResponse.newBuilder().addAllHooks(Arrays.asList(specs)).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }


    @Override
    public void onClientConnected(ClientConnectedRequest request, StreamObserver<EmptySuccess> responseObserver) {
        DEBUG("onClientConnected", request.getClientinfo().getClientid());
        var deviceEntityList = productDeviceService.findAllByClientId(request.getClientinfo().getClientid());
        if (!deviceEntityList.isEmpty()) {
            productDeviceService.updateOnlineByClientId("connected", deviceEntityList.get(0).getClientId());
        }

        EmptySuccess reply = EmptySuccess.newBuilder().build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void onClientDisconnected(ClientDisconnectedRequest request, StreamObserver<EmptySuccess> responseObserver) {
        DEBUG("onClientDisconnected", request.getClientinfo().getClientid());
        var deviceEntityList = productDeviceService.findAllByClientId(request.getClientinfo().getClientid());
        if (!deviceEntityList.isEmpty()) {
            productDeviceService.updateOnlineByClientId("disconnected", deviceEntityList.get(0).getClientId());
        }
        EmptySuccess reply = EmptySuccess.newBuilder().build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void onMessageDelivered(MessageDeliveredRequest request, StreamObserver<EmptySuccess> responseObserver) {
        long time = System.currentTimeMillis();
        dealThingsModel.deal(request.getClientinfo().getClientid(), time, request.getMessage().getPayload().toStringUtf8());
        EmptySuccess reply = EmptySuccess.newBuilder().build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }


}

