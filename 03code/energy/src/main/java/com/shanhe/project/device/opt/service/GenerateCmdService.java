package com.shanhe.project.device.opt.service;

import com.shanhe.common.utils.Crc16m;
import com.shanhe.framework.enums.TcpCharEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import org.springframework.stereotype.Component;

import java.util.Calendar;

/**
 * @author zhoubin
 * @date 2025/3/27
 */
@Component
public class GenerateCmdService {

    public static void main(String[] args) {

        String chksum = CodingUtil.energyCheckSum("0302010448512C3335D3000904010300150002C40B");
        System.out.println(chksum);
        // 设置输出模拟量 TODO
//        cmd56();
        // 校正模拟量数据 TODO
//        cmd76();

        // 设置空调工作模式 TODO
//        cmd30();
        // 红外学习 TODO
//        cmd59();

        // 修改日期时间 TODO
//        cmd37();
        // 读取全部存储指令 TODO
//        cmd5D();










        // 设置模块ID
//        cmd0C();
        // 设置系统数据上报时间
//        cmd50();
        // 设置云服务器IP地址
//         cmd5F();
        // 设置设备IP地址
//        cmd5E();
        // 设置输出开关量
//        cmd58();

        // 读取系统数据上报时间
//        cmd63();
        // 读取云服务器IP地址
//         cmd62();
        // 读取设备IP地址
//        cmd61();
        // 读取配置参数
//        cmd60();






        // 读取模拟量
//        cmd55();
        // 存储指令
//        cmd52();
        // 删除单条存储指令
//        cmd5B();
        // 删除全部存储指令
//        cmd5A();
        // 临时读取
//        cmd54();
        // 读取开关量
//        cmd57();
        // 设置配置参数
//        cmd51();
    }

    private static void cmd0C() {
        // 内容
        // 内容
        String info = "0000000001";

        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("3", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000002" // imei
                + "0C" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }


    private static void cmd5A() {
        // 内容
        // 内容
        String info = "AA";

        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("3", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "5A" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }
    private static void cmd5D() {
        // 内容
        // 内容
        String info = "AA";

        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("3", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "01" // 指令编号
                + "0000000001" // imei
                + "5D" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }
    private static void cmd5B() {
        // 内容
        // 内容
        String info = "01";

        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("3", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "01" // 指令编号
                + "0000000001" // imei
                + "5B" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    private static void cmd63() {
        // 内容

        String info = "";
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "63" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    private static void cmd5F() {
        // 内容

        String info = CodingUtil.stringToHexString("192", 2)
                + CodingUtil.stringToHexString("168", 2)
                + CodingUtil.stringToHexString("0", 2)
                + CodingUtil.stringToHexString("122", 2)

                + CodingUtil.stringToHexString("21123", 4);
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "5F" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    private static void cmd61() {
        // 内容
        String info = "";
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "61" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    private static void cmd62() {
        // 内容
        String info = "";
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "62" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    private static void cmd5E() {
        // 内容
        String info = CodingUtil.stringToHexString("192", 2)
                + CodingUtil.stringToHexString("168", 2)
                + CodingUtil.stringToHexString("0", 2)
                + CodingUtil.stringToHexString("202", 2)

                + CodingUtil.stringToHexString("255", 2)
                + CodingUtil.stringToHexString("255", 2)
                + CodingUtil.stringToHexString("255", 2)
                + CodingUtil.stringToHexString("0", 2)

                + CodingUtil.stringToHexString("192", 2)
                + CodingUtil.stringToHexString("168", 2)
                + CodingUtil.stringToHexString("0", 2)
                + CodingUtil.stringToHexString("1", 2)

                + CodingUtil.stringToHexString("5001", 4);
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "5E" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    private static void cmd37() {
        // 内容
        String info = "";
        Calendar calendar = Calendar.getInstance();
        info += CodingUtil.stringToHexString(String.valueOf(calendar.get(Calendar.YEAR)), 4);
        info += CodingUtil.stringToHexString(String.valueOf(calendar.get(Calendar.MONTH) + 1), 2);
        info += CodingUtil.stringToHexString(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), 2);
        info += CodingUtil.stringToHexString(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), 2);
        info += CodingUtil.stringToHexString(String.valueOf(calendar.get(Calendar.MINUTE)), 2);
        info += CodingUtil.stringToHexString(String.valueOf(calendar.get(Calendar.SECOND)), 2);
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "37" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());

    }

    /**
     * 设置输出模拟量
     */
    private static void cmd56() {
        // 内容
        String info = CodingUtil.stringToHexString("0", 2)
                + CodingUtil.stringToHexString("220", 4)
                + CodingUtil.stringToHexString("1", 2)
                + CodingUtil.stringToHexString("221", 4)
                + CodingUtil.stringToHexString("2", 2)
                + CodingUtil.stringToHexString("222", 4)
                + CodingUtil.stringToHexString("3", 2)
                + CodingUtil.stringToHexString("223", 4)
                + CodingUtil.stringToHexString("4", 2)
                + CodingUtil.stringToHexString("224", 4)
                + CodingUtil.stringToHexString("5", 2)
                + CodingUtil.stringToHexString("225", 2);
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "55" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    /**
     * 设置系统数据上报时间
     */
    private static void cmd50() {
        // 内容
        // 内容
        String info = CodingUtil.stringToHexString("60", 8);

        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "50" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    /**
     * 读取配置参数
     */
    private static void cmd52() {
        // 内容
        // 内容
        byte[] sbuf = Crc16m.getSendBuf("010300000002");
        String info = "01" + Crc16m.getBufHexStr(sbuf);

        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("3", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "01" // 指令编号
                + "0000000001" // imei
                + "52" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    /**
     * 读取配置参数
     */
    private static void cmd60() {
        // 内容
        String info = CodingUtil.stringToHexString("1", 2);

        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("3", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "60" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    /**
     * 设置配置参数
     */
    private static void cmd51() {
        // 内容
        String info = CodingUtil.stringToHexString("1", 2)
                + CodingUtil.stringToHexString("9600", 8)
                + CodingUtil.stringToHexString("3", 2)
                + CodingUtil.stringToHexString("0", 2)
                + CodingUtil.stringToHexString("0", 2)
                + CodingUtil.stringToHexString("60000", 4);

        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("3", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "51" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    /**
     * 临时读取
     */
    private static void cmd54() {
        // 内容
        byte[] sbuf = Crc16m.getSendBuf("010300000002");
        String info = Crc16m.getBufHexStr(sbuf);

        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("03", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "D4" // 指令编号
                + "0000000001" // imei
                + "54" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    /**
     * 读取模拟量
     */
    private static void cmd55() {
        // 内容
        String info = "01000102030405";
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "55" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

    /**
     * 读取开关量
     */
    private static void cmd57() {
        // 内容
        String info = CodingUtil.stringToHexString("1", 2);
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "57" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }


    /**
     * 设置输出开关量
     */
    private static void cmd58() {

        // 内容
        String info = "00010101020003000400050006000700";
        // 长度
        String length = CodingUtil.stringToHexString((info.length() / 2) + "", 4);

        String dataStr = CodingUtil.stringToHexString("0", 2)  // c0 设备类型
                + CodingUtil.stringToHexString("0", 2) // c1 串口号
                + CodingUtil.stringToHexString("1", 2)  // c2 模块地址
                + "00" // 指令编号
                + "0000000001" // imei
                + "58" // imei
                + length + info;
        // 校验码
        String chksum = CodingUtil.energyCheckSum(dataStr);

        System.out.println(TcpCharEnum._AA.getDictValue() + dataStr + chksum + TcpCharEnum._55.getDictValue());
    }

}
