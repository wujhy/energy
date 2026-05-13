package com.shanhe.framework.comm.tcp.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 编码处理
 *
 * @author wjh
 * @since 2025/7/19
 */
public class CodingUtil {

    private static final Logger logger = LoggerFactory.getLogger(CodingUtil.class);

    private static Map<String,String> weatherCodeList ;

    private static Map<String,String> countryCodeWithTimeZoneList ;
    /**
     *  根据天气描述获取天气编码
     */
    public static String getWeatherCode(String weatherDes) {
        if(weatherCodeList == null) {
            //0——晴? 1——阴?? 2——雨? 3——雪
            weatherCodeList = new HashMap<>();
            weatherCodeList.put("晴",	"0");
            weatherCodeList.put("多云",	"0");
            weatherCodeList.put("阴",	"1");
            weatherCodeList.put("阵雨",	"2");
            weatherCodeList.put("雷阵雨",	"2");
            weatherCodeList.put("雷阵雨并伴有冰雹",	"2");
            weatherCodeList.put("雨夹雪",	"2");
            weatherCodeList.put("小雨",	"2");
            weatherCodeList.put("中雨",	"2");
            weatherCodeList.put("大雨",	"2");
            weatherCodeList.put("暴雨",	"2");
            weatherCodeList.put("大暴雨",	"2");
            weatherCodeList.put("特大暴雨",	"2");
            weatherCodeList.put("阵雪",	"3");
            weatherCodeList.put("小雪",	"3");
            weatherCodeList.put("中雪",	"3");
            weatherCodeList.put("大雪",	"3");
            weatherCodeList.put("暴雪",	"3");
            weatherCodeList.put("雾",	"1");
            weatherCodeList.put("冻雨",	"2");
            weatherCodeList.put("沙尘暴",	"1");
            weatherCodeList.put("小雨-中雨",	"2");
            weatherCodeList.put("中雨-大雨	",	"2");
            weatherCodeList.put("大雨-暴雨",	"2");
            weatherCodeList.put("暴雨-大暴雨",	"2");
            weatherCodeList.put("大暴雨-特大暴雨",	"2");
            weatherCodeList.put("小雪-中雪",	"3");
            weatherCodeList.put("中雪-大雪",	"3");
            weatherCodeList.put("大雪-暴雪",	"3");
            weatherCodeList.put("浮尘",	"1");
            weatherCodeList.put("扬沙",	"1");
            weatherCodeList.put("强沙尘暴",	"1");
            weatherCodeList.put("飑",	"1");
            weatherCodeList.put("龙卷风",	"1");
            weatherCodeList.put("弱高吹雪",	"3");
            weatherCodeList.put("轻雾",	"1");
            weatherCodeList.put("霾",	"1");
        }
        String code = "4";
        for (String key : weatherCodeList.keySet()) {
            if(key.equals(weatherDes)) {
                code = weatherCodeList.get(weatherDes);
            }
        }

        if (code == null) {
            System.out.println("匹配不到对应的天气编码,用其他的  :4");

            code = "4";
        }
        return code;
    }

    /**
     * utf转gbk
     */
    public static String utf8ToGb2312(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    try {
                        sb.append((char) Integer.parseInt(
                                str.substring(i + 1, i + 3), 16));
                    }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException();
                    }
                    i += 2;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }

        try {
            byte[] inputBytes = sb.toString().getBytes(StandardCharsets.ISO_8859_1);
            return new String(inputBytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
        return null;
    }

    /**
     * GB2312 转 UTF-8
     */
    public static String gb2312ToUtf8(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {}
        return "";
    }

    /**
     * GBK2312 转 中文字符
     */
    public static String getGbk2312Format(String gbk2312Str) throws UnsupportedEncodingException {
        byte[] bytes = new byte[gbk2312Str.length() / 2];
        for(int i = 0; i < bytes.length; i ++){
            byte high = Byte.parseByte(gbk2312Str.substring(i * 2, i * 2 + 1), 16);
            byte low = Byte.parseByte(gbk2312Str.substring(i * 2 + 1, i * 2 + 2), 16);
            bytes[i] = (byte) (high << 4 | low);
        }
        return new String(bytes, "gbk");
    }

    /**
     * 随机生成字符串
     */
    public static String getRandomString(int length){
        //产生随机数
        Random random=new Random();
        StringBuilder sb=new StringBuilder();
        //循环length次
        for(int i = 0; i < length; i++) {
            //产生0-2个随机数，既与a-z，A-Z，0-9三种可能
            int number=random.nextInt(3);
            long result;
            switch(number){
                //如果number产生的是数字0；
                case 0:
                    //产生A-Z的ASCII码
                    result=Math.round(Math.random()*25+65);
                    //将ASCII码转换成字符
                    sb.append((char) result);
                    break;
                case 1:
                    //产生a-z的ASCII码
                    result=Math.round(Math.random()*25+97);
                    sb.append((char) result);
                    break;
                case 2:
                    //产生0-9的数字
                    sb.append(new Random().nextInt(10));
                    break;
                default:
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * 判断是否是 jsonStr
     */
    public static boolean isJson(String content) {
        try {
            JSONObject.parseObject(content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 格式 去掉 JSON字符串中的 bom报头
     */
    public static String formatJsonString(String s) {
        if (s != null) {
            s = s.replaceAll("\ufeff", "");
        }
        return s;
    }

    /**
     * 对字符串进行unicode编码
     */
    public static String unicode(String source){
        StringBuilder sb = new StringBuilder();
        char [] sourceChar = source.toCharArray();
        String unicode;
        for (char c : sourceChar) {
            unicode = Integer.toHexString(c);
            if (unicode.length() <= 2) {
                unicode = "00" + unicode;
            }
            sb.append(unicode.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 对字符串进行unicode解码
     */
    public static String decodeUnicode(String unicode) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < unicode.length(); i+=4) {
            int data = Integer.parseInt(unicode.substring(i, i+4), 16);
            sb.append((char) data);
        }
        return sb.toString();
    }

    /**
     * 对字符串进行unicode 解码    (有  "\\u"  的字符串) eg :  "\\u0043\\u0045"
     */
    public static String decodeUnicode2(String dataStr) {
        int start = 0;
        int end;
        final StringBuilder buffer = new StringBuilder();
        while (start > -1) {
            end = dataStr.indexOf("\\u", start + 2);
            String charStr;
            if (end == -1) {
                charStr = dataStr.substring(start + 2);
            } else {
                charStr = dataStr.substring(start + 2, end);
            }
            char letter = (char) Integer.parseInt(charStr, 16);
            buffer.append(new Character(letter));
            start = end;
        }
        return buffer.toString();
    }

    /**
     *  SJTC 协议的转义
     */
    public static byte[] escapeByteArr(byte[] byteArr) {
        byte [] escDataArr = byteArr.clone();
        int arrlength = byteArr.length;
        for (int i = 0; i < arrlength; i++) {
            switch (escDataArr[i]) {
                case 0x7D :
                    escDataArr = insertAt(escDataArr, i+1, (byte) 0x01);
                    i++;
                    arrlength++;
                    break;
                case 0X5B :
                    escDataArr[i] = 0x02;
                    escDataArr = insertAt(escDataArr, i, (byte) 0x7D);
                    i++;
                    arrlength++;
                    break;
                case 0X5D :
                    escDataArr[i] = 0x03;
                    escDataArr = insertAt(escDataArr, i, (byte) 0x7D);
                    i++;
                    arrlength++;
                    break;
                case 0X2C :
                    escDataArr[i] = 0x04;
                    escDataArr = insertAt(escDataArr, i, (byte) 0x7D);
                    i++;
                    arrlength++;
                    break;
                case 0X2A :
                    escDataArr[i] = 0x05;
                    escDataArr = insertAt(escDataArr, i, (byte) 0x7D);
                    i++;
                    arrlength++;
                    break;
                default :
                    break;
            }
        }
        return escDataArr;
    }

    /**
     * SJTC 协议的反转义
     */
    public static byte[] theEscapeByteArr(byte[] byteArr) {
        byte [] escDataArr = byteArr.clone();
        int arrlength = byteArr.length;
        for (int i = 0; i < arrlength; i++) {
            if(escDataArr[i] == 0x7D && (i+1) < arrlength) {
                switch (escDataArr[i+1]) {
                    case 0x01 :
//						escDataArr[i] = 0x7D;
                        escDataArr = deleteAt(escDataArr, i+1);
                        arrlength--;
                        break;
                    case 0x02 :
                        escDataArr[i] = 0X5B;
                        escDataArr = deleteAt(escDataArr, i+1);
                        arrlength--;
                        break;
                    case 0X03 :
                        escDataArr[i] = 0X5D;
                        escDataArr = deleteAt(escDataArr, i+1);
                        arrlength--;
                        break;
                    case 0X04 :
                        escDataArr[i] = 0X2C;
                        escDataArr = deleteAt(escDataArr, i+1);
                        arrlength--;
                        break;
                    case 0X05 :
                        escDataArr[i] = 0x2A;
                        escDataArr = deleteAt(escDataArr, i+1);
                        arrlength--;
                        break;
                    default :
                        break;
                }
            }
        }

        return escDataArr;

    }

    /**
     *  删除byte数组里的某个元素
     */
    public static byte[] deleteAt(byte[] bs, int index)
    {
        int length = bs.length - 1;
        byte[] ret = new byte[length];

        System.arraycopy(bs, 0, ret, 0, index);
        System.arraycopy(bs, index + 1, ret, index, length - index);

        return ret;
    }

    /**
     * byte数组里的某个位置插入元素
     */
    public static byte[] insertAt(byte[] bs, int index ,byte item)
    {
        int length = bs.length + 1;
        byte[] ret = new byte[length];

        System.arraycopy(bs, 0, ret, 0, index);
        ret[index] = item;
        System.arraycopy(bs, index , ret, index+1, length - (index+1));

        return ret;
    }

    /**
     * 得到amr的时长
     */
    public static int getAmrDuration(byte [] fileByte) throws IOException {
        long duration = -1;
        int[] packedSize = { 12, 13, 15, 17, 19, 20, 26, 31, 5, 0, 0, 0, 0, 0, 0, 0 };

        final File file = File.createTempFile("temp"+CodingUtil.getRandomString(16), ".amr");//创建临时文件
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(fileByte);
        //关闭临时文件
        fos.flush();
        fos.close();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            long length = file.length();// 文件的长度
            int pos = 6;// 设置初始位置
            int frameCount = 0;// 初始帧数
            int packedPos = -1;

            byte[] data = new byte[1];// 初始数据值
            while (pos <= length) {
                randomAccessFile.seek(pos);
                if (randomAccessFile.read(data, 0, 1) != 1) {
                    duration = (length - 6) / 650;
                    break;
                }
                packedPos = (data[0] >> 3) & 0x0F;
                pos += packedSize[packedPos] + 1;
                frameCount++;
            }

            duration += frameCount * 20L;// 帧数*20
        }
//		System.out.println("duration : "+duration/1000.0);
//		System.out.println("四舍五入后的 :"+Math.round(duration/1000.0));
        //  向上取整用Math.ceil(1.21)  >>> 2.0
        //	向下取整用Math.floor(1.61) >>> 1.0
        //删除临时文件
        file.delete();
        return (int)(Math.ceil(duration/1000.0));
    }

    /**
     * 收集异常堆栈信息
     */
    public static String collectExceptionStackMsg(Exception e){
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }



    /**
     * byte[]数组转换为16进制的字符串
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的结果
     */
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 字节数组转字符串
     */
    public static String bytesToString(byte[] b, String encode) throws Exception {
        return new String(b, encode);
    }

    /**
     *16进制字符串转为字节数组
     */
    public static byte[] hexToByte(String hex){
        //先去掉16进制字符串的空格
        hex = hex.replace(" ","");
        //字节数组长度为16进制字符串长度的一半
        int byteLength = hex.length()/2;
        byte[] bytes = new byte[byteLength];
        int m, n;
        for(int i = 0; i<byteLength;i++){
            m = i*2+1;
            n = m+1;
            int intHex = Integer.decode("0x"+hex.substring(i * 2, m) + hex.substring(m, n));
            if (intHex < 0){
                intHex = 256 - intHex;
            }
            bytes[i] = (byte) intHex;
        }
        return bytes;
    }

    /**
     * 10进制字符串转为16进制字符串
     * @param string 字符串
     * @param num 多少位
     * @return 16进制字符串
     */
    public static String stringToHexString(String string, Integer num) {
        String hexString = StrUtil.isNotBlank(string) ? Integer.toHexString(Integer.parseInt(string)) : "0";
        return String.format("%" + num + "s", hexString).replace(" ","0");
    }

    /**
     * 10进制转为16进制字符串
     * @param value 值
     * @param num 多少位
     * @return 16进制字符串
     */
    public static String integerToHexString(Integer value, Integer num) {
        String hexString = Integer.toHexString(value == null ? 0 : value);
        return String.format("%" + num + "s", hexString).replace(" ","0");
    }

    /**
     *16进制字符串转为10进制字符串
     */
    public static String hexStringToString(String hexString) {
        String result = String.valueOf(Integer.parseInt(hexString, 16));

        if (result.length() % 2 == 1) {
            result = "0" + result;
        }

        return result;
    }

    /**
     *16进制字符串转为10进制
     */
    public static Integer hexStringToInteger(String hexString) {
        return Integer.parseInt(hexString, 16);
    }

    /**
     *16进制字符串转为2进制字符串
     */
    public static String hexString2binaryString(String hexString) {
        //16进制转10进制
        BigInteger sInt = new BigInteger(hexString, 16);
        //10进制转2进制
        StringBuilder result = new StringBuilder(sInt.toString(2));
        int num = 8 - result.length();
        if (result.length() < 8) {
            for (int i = 0; i < num; i++) {
                result.insert(0, "0");
            }
        }
        return result.toString();
    }



    /**
     * 十进制转换成二进制 ()
     */
    public static String decimalToBinary(int decimalSource) {
        BigInteger bi = new BigInteger(String.valueOf(decimalSource));	//转换成BigInteger类型
        return bi.toString(2);	//参数2指定的是转化成X进制，默认10进制
    }

    /**
     * 二进制转换成十进制
     */
    public static int binaryToDecimal(String binarySource) {
        BigInteger bi = new BigInteger(binarySource, 2);    //转换为BigInteger类型
        return Integer.parseInt(bi.toString());        //转换成十进制
    }

    /**
     * 转换为Ascii码
     */
    public static String hexToAscii(String hex) {
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String part = hex.substring(i, i + 2);
            ascii.append((char) Integer.parseInt(part, 16));
        }
        return ascii.toString();
    }

    /**
     * 二进制转换成十六进制
     */
    public static String bin2hex(String input) {
        StringBuilder sb = new StringBuilder();
        int len = input.length();
        //System.out.println("原数据长度：" + (len / 8) + "字节");

        for (int i = 0; i < len / 4; i++){
            //每4个二进制位转换为1个十六进制位
            String temp = input.substring(i * 4, (i + 1) * 4);
            int tempInt = Integer.parseInt(temp, 2);
            String tempHex = Integer.toHexString(tempInt).toUpperCase();
            sb.append(tempHex);
        }

        return sb.toString();
    }

    /**
     * 16进制字符串转为float （大端序）
     */
    public static float hexToFloat(String hex) {
        // 去除可能的前缀(如0x)
        hex = hex.replace("0x", "").replace("0X", "");

        // 确保字符串长度为8个字符（4个字节）
        if (hex.length() != 8) {
            throw new IllegalArgumentException("Hex string must be 8 characters long for a float");
        }

        // 将十六进制字符串解析为整数
        int intBits = Integer.parseInt(hex, 16);

        // 将整数位模式解释为浮点数
        return Float.intBitsToFloat(intBits);
    }

    /**
     * 十六进制字符串转Float（大端序）
     * @param hexString 16进制字符串
     * @return 解析出的float值
     */
    public static float hexToFloatBigEndian(String hexString) {
        // 去除可能的前缀(如0x)
        hexString = hexString.replace("0x", "").replace("0X", "");
        // 确保字符串长度为8个字符（4个字节）
        if (hexString.length() != 8) {
            throw new IllegalArgumentException("Hex string must be 8 characters long for a float");
        }

        // 将16进制字符串转换为字节数组
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }

        // 使用ByteBuffer解析（大端序）
        return ByteBuffer.wrap(bytes).getFloat();
    }

    /**
     * 十六进制字符串转Float（小端序）
     * @param hexString 16进制字符串，例如"6666CC43"
     * @return 解析出的float值
     */
    public static float hexToFloatLittleEndian(String hexString) {
        // 去除可能的前缀(如0x)
        hexString = hexString.replace("0x", "").replace("0X", "");
        // 确保字符串长度为8个字符（4个字节）
        if (hexString.length() != 8) {
            throw new IllegalArgumentException("Hex string must be 8 characters long for a float");
        }

        // 将16进制字符串转换为字节数组
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }

        // 使用ByteBuffer解析（小端序）
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * 十六进制字符串转uint32，通过IEEE 754 float解析
     * @param hexString 16进制字符串
     * @return 解析后的int值（uint32表示的数值）
     */
    public static int hexToUint32AsFloat(String hexString) {
        // 去除可能的前缀
        hexString = hexString.replace("0x", "").replace("0X", "");

        // 确保字符串长度为8个字符（4个字节）
        if (hexString.length() != 8) {
            throw new IllegalArgumentException("Hex string must be 8 characters long for a 32-bit value");
        }

        try {
            // 将十六进制字符串解析为整数位模式
            int intBits = Integer.parseInt(hexString, 16);

            // 将整数位模式解释为IEEE 754单精度浮点数
            float floatValue = Float.intBitsToFloat(intBits);

            // 四舍五入转换为整数
            return Math.round(floatValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex string: " + hexString, e);
        }
    }

    /**
     * 十六进制字符串转无符号16位整形
     */
    public static int hexToU16(String hexString) {
        // 去除可能的前缀
        hexString = hexString.replace("0x", "").replace("0X", "");
        // 将十六进制字符串转换为BigInteger
        BigInteger bigInteger = new BigInteger(hexString, 16);
        // 检查是否在u16的范围内
        if (bigInteger.compareTo(BigInteger.ZERO) < 0 || bigInteger.compareTo(new BigInteger("FFFF", 16)) > 0) {
            logger.info("值{}超出u16的范围", hexString);
            return 0;
        } else {
            // 使用BigInteger进行模运算确保值在u16的范围内，相当于 2^16
            bigInteger = bigInteger.mod(new BigInteger("10000", 16));
            return bigInteger.intValue();
        }
    }

    /**
     * 十六进制字符串转16位整形
     */
    public static int hexToI16(String hexString) {
        int num = Integer.parseInt(hexString, 16);
        if (num >= 32768) {
            num = num - 65536;
        }
        return num;
    }

    /**
     * 十六进制字符串转32位整形
     */
    public static int hexToI32(String hexString) {
        // 将十六进制字符串转换为BigInteger
        BigInteger bigInteger = new BigInteger(hexString, 16);
        return bigInteger.intValue();
    }

    /**
     * 十六进制字符串转无符号32位整形
     */
    public static int hexToU32(String hexString) {
        // 去除可能的前缀
        hexString = hexString.replace("0x", "").replace("0X", "");
        // 将十六进制字符串转换为BigInteger
        BigInteger bigInteger = new BigInteger(hexString, 16);
        // 检查是否在u32的范围内
        if (bigInteger.compareTo(BigInteger.ZERO) < 0 || bigInteger.compareTo(new BigInteger("FFFFFFFF", 16)) > 0) {
            logger.info("值{}超出u32的范围", hexString);
            return 0;
        } else {
            // 使用BigInteger进行模运算确保值在u32的范围内，相当于 2^32
            bigInteger = bigInteger.mod(new BigInteger("100000000", 16));
            return bigInteger.intValue();
        }
    }

    /**
     * 计算校验和
     */
    public static String getCheckSum(String dataStr){
//		String str  = "0052 0901 05000000000000080017D6D11FE202106241708090";
        String str = dataStr.replace(" ", "");
        char[] chr;
        chr = str.toCharArray();
        int num = 0;
        for (char c : chr) {
            num += c;
        }
        String format = String.format("%X", num);
        return format.substring(1);
    }

    /**
     * 计算 异或和 校验位
     */
    public static int getOrCheck(byte[] b) {

        byte x = 0;
        for (byte value : b) {
            x ^= value;
        }
        return x;
    }

    /**
     * data1 与 data2拼接的结果
     */
    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;

    }

    public static boolean isNumeric(Object value){
        if(value instanceof Double || value instanceof Integer || value instanceof Long){
            return true;
        } else {
            String pattern = "^[\\+\\-]?[\\d]+(\\.[\\d]+)?$";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(String.valueOf(value));
            return m.matches();
        }
    }

    public static DateFormat getAlternativeIso8601DateFormat(String dataFormat) {
        if(dataFormat == null){
            dataFormat = "YYYY-MM-dd,HH:mm:ss";
        }
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat(dataFormat, Locale.US);
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df;
    }

    /**
     * 小数点偏移
     * @param num: 数值
     * @param pattern: #.000 (格式化)
     * @param calcNum: 1000
     **/
    public static String decimal(int num, String pattern, int calcNum){
        DecimalFormat decimalFormat = new DecimalFormat(pattern);
        String format = decimalFormat.format(Integer.valueOf(num).doubleValue() / calcNum);
        // 处理小数点前无数字的情况
        String i = format.substring(0,1);
        if (StrUtil.equals(i, ".")){
            // .xxx
            format = "0" + format;
        } else if (StrUtil.equals(i, "-") && format.charAt(1) == '.') {
            // 负数：-.xxx
            format = format.charAt(0) + "0" + format.substring(1);
        }
        return format;
    }

    /**
     * 能耗校验和
     */
    public static String energyCheckSum(String tempStr){
        int data = 0;
        for (int i = 0; i < tempStr.length()/2; i++) {
            String itemStr = tempStr.substring(i*2,i*2+2);
            data += Integer.parseInt(itemStr, 16);
        }

        String binary = decimalToBinary(data);
        if (binary.length() > 8) {
            binary = binary.substring(binary.length() - 8);
        } else if (binary.length() < 8) {
            int len = 8 - binary.length();
            for (int i = 0; i < len ; i++) {
                binary = "0" + binary;
            }
        }

        return bin2hex(binary);
    }

    /**
     * 读取一个文本 一行一行读取
     */
    public static List<String> readFile02(String path) throws IOException {
        // 使用一个字符串集合来存储文本中的路径 ，也可用String []数组
        List<String> list = new ArrayList<>();
        FileInputStream fis = new FileInputStream(path);
        // 防止路径乱码   如果utf-8 乱码  改GBK     eclipse里创建的txt  用UTF-8，在电脑上自己创建的txt  用GBK
        InputStreamReader isr = new InputStreamReader(fis, "GBK");
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            // 如果 t x t文件里的路径 不包含---字符串       这里是对里面的内容进行一个筛选
            if (line.lastIndexOf("---") < 0) {
                // 将数据添加到集合里
                if (line.startsWith("Receiving: AA")) {
                    list.add(line);
                }
                //list.add(line);
            }
        }
        br.close();
        isr.close();
        fis.close();
        return list;
    }

    /**
     * 字符串转Double类型
     */
    public static Double decimalDouble(Integer num, int calcNum){
        if (num == null) {
            return null;
        }
        if(calcNum == 0){
            return num.doubleValue();
        }
        return num.doubleValue() / calcNum;
    }

    /**
     * 字符串转Double类型
     */
    public static Double valueOfDouble(String str){
        if(StrUtil.isBlank(str)){
            return null;
        }
        return Double.parseDouble(str);
    }

    /**
     * 计算校验码 - 所有字节的模256的和
     */
    public static String check256(String tempStr) {
        if (tempStr == null || tempStr.isEmpty()) {
            return "";
        }
        // 去除空格
        tempStr = tempStr.replaceAll(" ", "");
        byte[] data = new byte[tempStr.length() / 2];

        for (int i = 0; i < tempStr.length() / 2; i++) {
            String itemStr = tempStr.substring(i * 2, i * 2 + 2);
            // 使用16进制解析
            data[i] = (byte) Integer.parseInt(itemStr, 16);
        }

        int count = 0;
        int len = data.length;
        for (int i = 0; i < len; i++) {
            // 使用 & 0xFF 确保按无符号字节处理
            count += data[i] & 0xFF;
        }
        // 模256运算，等同于与0xFF进行按位与操作
        int checksum = count & 0xFF;
        // 转换为16进制字符串，确保是两位数
        return String.format("%02X", checksum);
    }

    /**
     * 小数点位移方法
     *
     * @param num 位移对象
     * @param itemPoint 小数点位数
     * @return 位移后的对象
     */
    public static Object shiftDecimal(int num, int itemPoint) {
        if (itemPoint <= 0) {
            return num;
        }
        BigDecimal decimalValue = new BigDecimal(num);
        BigDecimal shiftedValue = decimalValue.divide(BigDecimal.valueOf(Math.pow(10, itemPoint)), itemPoint, RoundingMode.HALF_UP);
        return shiftedValue.toString();
    }

    public static int hexParseInt(String hexString) {
        return Integer.parseInt(hexString, 16);
    }

    /**
     * 生成采集指令模板编号
     * @param notCodes 排除的编码
     * @param num 位数
     */
    public static String generateHexCode(List<String> notCodes, Integer num) {
        if (notCodes == null) {
            notCodes = new ArrayList<>();
        }
        if (num == null) {
            num = 1;
        }

        int maxNum = 16;
        for (int i = 1; i < num; i++) {
            maxNum *= 16;
        }

        if (notCodes.size() >= maxNum) {
            throw new ServiceException("设备已存在" + maxNum + "条采集指令模板，请删除后重试");
        }

        String result = null;
        for (int i = 1; i < maxNum; i++) {
            result = CodingUtil.integerToHexString(i, num).toUpperCase();
            if (!notCodes.contains(result)) {
                break;
            }
        }

        if (StrUtil.isBlank(result)) {
            throw new ServiceException("设备已存在" + maxNum + "条采集指令模板，请删除后重试");
        }

        return result;
    }

    /**
     * 解析补码形式
     * @param value 值
     * @param radix 进制类型    16：16进制
     * @param bits 位数
     * @return 解析后的值
     */
    public static int parseComplement(String value, int radix, int bits) {
        // 将十六进制字符串转换为整数
        int intValue = Integer.parseInt(value, radix);

        bits = bits * 8;

        // 判断是否为负数
        // 计算最大正数值
        int maxPositiveValue = (1 << (bits - 1)) - 1;

        // 判断是否为负数
        if (intValue > maxPositiveValue) {
            // 负数，转换为补码形式
            intValue = intValue - (1 << bits);
        }


        // 返回温度值（单位：0.1°C）
        return intValue;
    }

    /**
     * 转换Integer类型
     */
    public static Integer valueOfInteger(String str){
        if(StrUtil.isBlank(str)){
            return null;
        }
        return Integer.parseInt(str);
    }

}
