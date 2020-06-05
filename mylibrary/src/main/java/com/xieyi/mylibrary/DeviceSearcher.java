package com.xieyi.mylibrary;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.xieyi.mylibrary.uitl.CRC16Util;

import org.ipps.base.device.IPDeviceInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceSearcher {
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static Handler uiHandler = new Handler(Looper.getMainLooper());

    /**
     * 开始搜索
     *
     * @param onSearchListener
     */
    public static void search(OnSearchListener onSearchListener) {
        executorService.execute(new SearchRunnable(onSearchListener));
    }

    public interface OnSearchListener {
        void onSearchStart();

        void onSearchedNewOne(IPDeviceInfo ipDeviceInfo);

        void onSearchFinish();
    }


    private static class SearchRunnable implements Runnable {

        OnSearchListener searchListener;

        public SearchRunnable(OnSearchListener listener) {
            this.searchListener = listener;
        }

        @Override
        public void run() {
            DatagramSocket socket = null;
            try {
                if (searchListener != null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            searchListener.onSearchStart();
                        }
                    });
                }
                socket = new DatagramSocket();
                //设置接收等待时长
                socket.setSoTimeout(4000);
                byte[] sendData = new byte[1024];
                byte[] receData = new byte[1024];
                DatagramPacket recePack = new DatagramPacket(receData, receData.length);
                //使用广播形式（目标地址设为255.255.255.255）的udp数据包
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 5002);
                //用于存放已经应答的设备
                HashMap<String, IPDeviceInfo> devices = new HashMap<>();
                sendPacket.setData(packSearchData());
                //发送udp数据包
                socket.send(sendPacket);
                try {
                    //限定搜索设备的最大数量
                    int rspCount = 250;
                    while (rspCount > 0) {
                        socket.receive(recePack);
                        final IPDeviceInfo device = parseRespData(recePack);

                        if (devices.get(device.getHost()) == null) {
                            //保存新应答的设备
                            devices.put(device.getHost(), device);
                            if (searchListener != null) {
                                uiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        searchListener.onSearchedNewOne(device);
                                    }
                                });
                            }
                        }
                        rspCount--;
                    }
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                }
                socket.close();
                if (searchListener != null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            searchListener.onSearchFinish();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (socket != null) {
                    socket.close();
                }
                if (searchListener != null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            searchListener.onSearchFinish();
                        }
                    });
                }
            }
        }

        /**
         * 校验和解析应答的数据包
         *
         * @param pack udp数据包
         * @return
         */
        private IPDeviceInfo parseRespData(DatagramPacket pack) {
            if (pack.getLength() < 2) {
                return null;
            }

            byte[] data = pack.getData();


            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                sb.append(String.format("%02x", data[i]));
            }

            Log.e("搜索秤后收到的数据", sb.toString());

            byte[] bytes = Arrays.copyOfRange(data, 21, 23);
            byte[] bytes2 = Arrays.copyOfRange(data, 17, 19);
            int i = CRC16Util.byteArrayToInt(bytes);
            int j = CRC16Util.byteArrayToInt(bytes2);

            if (i == 0 && j == 0) {
                byte[] num = Arrays.copyOfRange(data, 93, 95);
                return new IPDeviceInfo(String.valueOf(CRC16Util.byteArrayToInt(num)), "", "", "", pack.getAddress().getHostAddress());
            } else {
                return new IPDeviceInfo(String.valueOf(i), "", "", "", pack.getAddress().getHostAddress());
            }
        }

        /**
         * 生成搜索数据包
         * 格式：$(1) + packType(1) + sendSeq(4) + dataLen(1) + [data]
         * packType - 报文类型
         * sendSeq - 发送序列
         * dataLen - 数据长度
         * data - 数据内容
         *
         * @return
         */
        private byte[] packSearchData() {
            byte[] data = new byte[17];
            int offset = 0;
//			data[offset++] = RemoteConst.PACKET_PREFIX;
//			data[offset++] = RemoteConst.PACKET_TYPE_SEARCH_DEVICE_REQ;
//			data[offset++] = (byte) seq;
//			data[offset++] = (byte) (seq >> 8);
//			data[offset++] = (byte) (seq >> 16);
//			data[offset++] = (byte) (seq >> 24);
//			data[offset++] = (byte) (0x11);

            data[offset++] = (byte) (0x03);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x0b);
            data[offset++] = (byte) (0x80);
            data[offset++] = (byte) (0x10);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x00);
            data[offset++] = (byte) (0x9b);
            data[offset++] = (byte) (0x04);
            return data;
        }
    }
}
