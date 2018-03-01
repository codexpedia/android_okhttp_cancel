package com.example.cancelokhttp;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class OkHttpUtils {
//    public static void cancelCallWithTag(OkHttpClient client, String tag) {
//        for(Call call : client.dispatcher().queuedCalls()) {
//            if(call.request().tag().equals(tag))
//                call.cancel();
//        }
//        for(Call call : client.dispatcher().runningCalls()) {
//            if(call.request().tag().equals(tag))
//                call.cancel();
//        }
//    }


    public static void cancelCallWithTag(OkHttpClient client, String tag) {
        System.out.println("cancelCallWithTag>>>" + tag);

        System.out.println("client.dispatcher().queuedCalls().size()>>>" + client.dispatcher().queuedCalls().size());
        System.out.println("client.dispatcher().runningCalls().size()>>>" + client.dispatcher().runningCalls().size());


        for(Call call : client.dispatcher().queuedCalls()) {
            System.out.println("queuedCalls call.request().tag()>>>>>" + call.request().tag());
            if (call.request().tag().equals(tag)) {
                call.cancel();
                System.out.println("call.cancel()>>>>>" + call.request().tag());
            }
        }

        for(Call call : client.dispatcher().runningCalls()) {
            System.out.println("runningCalls call.request().tag()>>>>>" + call.request().tag());
            if (call.request().tag().equals(tag)) {
                call.cancel();
                System.out.println("runningCalls call.cancel()>>>>>" + call.request().tag());
            }
        }

    }


}