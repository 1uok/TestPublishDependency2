package com.xieyi.mylibrary;

import android.os.Handler;
import android.os.Looper;

import com.xieyi.mylibrary.uitl.BcdUtil;
import com.xieyi.mylibrary.uitl.CRC16Util;
import com.xieyi.mylibrary.uitl.DoubleUtil;

import org.ipps.base.scale.info.PluInfo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceSender {

    public static boolean isCancle;

    private static int progress = 0;

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static Handler uiHandler = new Handler(Looper.getMainLooper());
    private static int mCount = 1;
    private static boolean isContinue;
    private static int packageNum;

    public static void send(String ip, List<PluInfo> list, OnSendListener onSendListener) {
        executorService.execute(new SendRunnable(ip,list,onSendListener));
    }

    public interface OnSendListener {

        void onSendProgress(int progress);

    }

    private static class SendRunnable implements Runnable {
        List<PluInfo> list;
        OnSendListener mOnSendListener;
        String ip;

        public SendRunnable(String ip,List<PluInfo> list,OnSendListener onSendListener){
            this.ip = ip;
            this.list = list;
            this.mOnSendListener = onSendListener;
        }

        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket();
                //设置接收等待时长
                socket.setSoTimeout(5000);
                byte[] sendData = new byte[1024];
                byte[] receData = new byte[50];
                DatagramPacket recePack = new DatagramPacket(receData, receData.length);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ip), 5002);

                progress = 0;
                int downloadCount = 0;
                for (int i = 0; i < list.size(); i++) {
                    if(!isCancle){
                        packageNum = i;
                        byte[] byteArray = getByteArray(list.get(i), i);
                        sendPacket.setData(byteArray);
                        //发送udp数据包
                        mCount = 1;
                        boolean isContinue = sendAndReceive(sendPacket, recePack, socket);
                        if(!isContinue){
                            if (mOnSendListener != null) {
                                uiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mOnSendListener.onSendProgress(-1);
                                    }
                                });
                            }
                            break;
                        }

                        if(i == list.size() - 1){
                            if (mOnSendListener != null) {
                                uiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mOnSendListener.onSendProgress(100);
                                    }
                                });
                            }
                        }else{
                            progress = (int) (((float) i / list.size()) * 100);
                            if (progress > downloadCount) {
                                downloadCount ++;
                                if (mOnSendListener != null) {
                                    uiHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mOnSendListener.onSendProgress(progress);
                                        }
                                    });
                                }
                            }
                        }
                    }else{
                        if (mOnSendListener != null) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mOnSendListener.onSendProgress(-1);
                                }
                            });
                        }
                        break;
                    }

                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean sendAndReceive(DatagramPacket sendPacket,DatagramPacket recePack,DatagramSocket socket){
            try {
                socket.send(sendPacket);
                try {
                    socket.receive(recePack);
                    if (!parseRespData(recePack)) {
                        throw new SocketTimeoutException("连接超时");
                    }else{
                        isContinue = true;
                    }
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    if(mCount <= 2){
                        ++mCount;
                        sendAndReceive(sendPacket,recePack,socket);
                    }else{
                        isContinue = false;
                        return false;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                isContinue = false;
                return false;
            }
            return isContinue;
        }

        /**
         * 校验和解析应答的数据包
         *
         * @param pack udp数据包
         * @return
         */
        private boolean parseRespData(DatagramPacket pack) {
            if (pack.getLength() < 2) {
                return false;
            }
            //这边不做数据校验的原因有两点，1：如果发送失败，不管是什么原因导致的失败，接收方都不会返数据给我们，所以我们可以默认收到数据就成功  2：就算校验包名的话，如果发生超时错误后又收到数据会导致一直错下去，很影响效率，基本上3秒内都能返回，3秒外的基本都是返回不了的了

            //StringBuilder sb = new StringBuilder();
            byte[] data = pack.getData();
//            for (int i = 0; i < data.length; i++) {
//                sb.append(String.format("%02x", data[i]));
//            }
//            Log.e("收到的数据",sb.toString());
            byte[] packageNumBytes = new byte[4];
            System.arraycopy(data, 1, packageNumBytes, 0, 4);
            int i = CRC16Util.byteArrayToIntFour(packageNumBytes);
            if(i != packageNum){
//                Log.e("走到了no不正确","11111111111");
//                Log.e("no是什么",i + "");
//                Log.e("package是什么",packageNum + "");
                return false;
            }
            return true;
        }


        private byte[] getByteArray(PluInfo data, int count) {

            byte[] sendByte = new byte[531];
            //stx-etx长度17
            byte[] b1 = new byte[17];
            b1[0] = (byte) 0x03;
            b1[1] = (byte) (count >> 24 & 0xff);
            b1[2] = (byte) (count >> 16 & 0xff);
            b1[3] = (byte) (count >> 8 & 0xff);
            b1[4] = (byte) (count & 0xff);
            b1[5] = (byte) 0x00;
            b1[6] = (byte) 0x0a;
            b1[7] = (byte) 0x90;
            b1[8] = (byte) 0x00;
            b1[13] = (byte) 0x02;
            b1[14] = (byte) 0x02;
            for (int i = 1; i < 15; i++) {
                b1[15] += b1[i];
            }
            b1[16] = (byte) 0x04;


            byte[] dataByte = new byte[512];
            byte[] bytes = BcdUtil.str2Bcd(String.valueOf(data.getPlu()));
            System.arraycopy(bytes, 0, dataByte, 0, 3);

            try {
                byte[] bytes1 = String.valueOf(data.getPlu()).getBytes("GBK");
                System.arraycopy(bytes1, 0, dataByte, 3, bytes1.length);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                byte[] bytes2 = String.valueOf(data.getBarcodeId()).getBytes("GBK");
                System.arraycopy(bytes2, 0, dataByte, 3 + 16, bytes2.length);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            dataByte[39] = (byte) 0x16;
            try {
                byte[] bytes3 = data.getName().getBytes("GBK");
                System.arraycopy(bytes3, 0, dataByte, 3 + 16 + 20 + 2, bytes3.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
            long price = (long) DoubleUtil.mulLong(data.getPrice(), 100L);
            byte[] bytes4 = CRC16Util.toHH6(price);
            System.arraycopy(bytes4, 0, dataByte, 161, bytes4.length);
            byte[] bytes5 = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00};
            System.arraycopy(bytes5, 0, dataByte, 167, bytes5.length);
            if (data.getSaleType() == 1) {
                dataByte[184] = (byte) 0x01;
            } else {
                dataByte[184] = (byte) 0x00;
            }

            dataByte[185] = (byte) 0x81;

            byte[] crc = CRC16Util.CRC_XModem(dataByte);
            System.arraycopy(b1, 0, sendByte, 0, 17);
            System.arraycopy(dataByte, 0, sendByte, 17, 512);
            System.arraycopy(crc, 0, sendByte, 529, 2);

            return sendByte;
        }
    }
}
