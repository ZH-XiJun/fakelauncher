package com.wtbruh.fakelauncher.utils;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 中国农历工具类-
 */
public class LunarCalender {
    private int year; // 农历的年份
    private int month;
    private int day;
    private String lunarMonth; // 农历的月份
    public static int leapMonth = 0; // 闰的是哪个月

    final static String[] chineseNumber = { "正", "二", "三", "四", "五", "六", "七",
            "八", "九", "十", "冬", "腊" };
    static SimpleDateFormat chineseDateFormat = new SimpleDateFormat(
            "yyyy年MM月dd日");
    final static long[] lunarInfo = new long[] {
            0x4bd8, 0x4ae0, 0xa570, 0x54d5, 0xd260, 0xd950, 0x5554, 0x56af, 0x9ad0, 0x55d2,
            0x4ae0, 0xa5b6, 0xa4d0, 0xd250, 0xd255, 0xb54f, 0xd6a0, 0xada2, 0x95b0, 0x4977,
            0x497f, 0xa4b0, 0xb4b5, 0x6a50, 0x6d40, 0xab54, 0x2b6f, 0x9570, 0x52f2, 0x4970,
            0x6566, 0xd4a0, 0xea50, 0x6a95, 0x5adf, 0x2b60, 0x86e3, 0x92ef, 0xc8d7, 0xc95f,
            0xd4a0, 0xd8a6, 0xb55f, 0x56a0, 0xa5b4, 0x25df, 0x92d0, 0xd2b2, 0xa950, 0xb557,
            0x6ca0, 0xb550, 0x5355, 0x4daf, 0xa5b0, 0x4573, 0x52bf, 0xa9a8, 0xe950, 0x6aa0,
            0xaea6, 0xab50, 0x4b60, 0xaae4, 0xa570, 0x5260, 0xf263, 0xd950, 0x5b57, 0x56a0,
            0x96d0, 0x4dd5, 0x4ad0, 0xa4d0, 0xd4d4, 0xd250, 0xd558, 0xb540, 0xb6a0, 0x95a6,
            0x95bf, 0x49b0, 0xa974, 0xa4b0, 0xb27a, 0x6a50, 0x6d40, 0xaf46, 0xab60, 0x9570,
            0x4af5, 0x4970, 0x64b0, 0x74a3, 0xea50, 0x6b58, 0x5ac0, 0xab60, 0x96d5, 0x92e0,
            0xc960, 0xd954, 0xd4a0, 0xda50, 0x7552, 0x56a0, 0xabb7, 0x25d0, 0x92d0, 0xcab5,
            0xa950, 0xb4a0, 0xbaa4, 0xad50, 0x55d9, 0x4ba0, 0xa5b0, 0x5176, 0x52bf, 0xa930,
            0x7954, 0x6aa0, 0xad50, 0x5b52, 0x4b60, 0xa6e6, 0xa4e0, 0xd260, 0xea65, 0xd530,
            0x5aa0, 0x76a3, 0x96d0, 0x4afb, 0x4ad0, 0xa4d0, 0xd0b6, 0xd25f, 0xd520, 0xdd45,
            0xb5a0, 0x56d0, 0x55b2, 0x49b0, 0xa577, 0xa4b0, 0xaa50, 0xb255, 0x6d2f, 0xada0,
            0x4b63, 0x937f, 0x49f8, 0x4970, 0x64b0, 0x68a6, 0xea5f, 0x6b20, 0xa6c4, 0xaaef,
            0x92e0, 0xd2e3, 0xc960, 0xd557, 0xd4a0, 0xda50, 0x5d55, 0x56a0, 0xa6d0, 0x55d4,
            0x52d0, 0xa9b8, 0xa950, 0xb4a0, 0xb6a6, 0xad50, 0x55a0, 0xaba4, 0xa5b0, 0x52b0,
            0xb273, 0x6930, 0x7337, 0x6aa0, 0xad50, 0x4b55, 0x4b6f, 0xa570, 0x54e4, 0xd260,
            0xe968, 0xd520, 0xdaa0, 0x6aa6, 0x56df, 0x4ae0, 0xa9d4, 0xa4d0, 0xd150, 0xf252,
            0xd520};

    // 农历部分假日
    final static String[] lunarHoliday = new String[] {
            "0101 春节",
            "0115 元宵节",
            "0505 端午节",
            "0707 七夕情人节",
            "0715 中元节 孟兰节",
            "0730 地藏节",
            "0802 灶君诞",
            "0815 中秋节",
            "0827 先师诞",
            "0909 重阳节",
            "1208 腊八节  释迦如来成道日",
            "1223 小年",
            "0100 除夕" };

    // 公历部分节假日
    final static String[] solarHoliday = new String[] {
            "0101 元旦",
            "0110 中国110宣传日",
            "0214 情人",
            "0221 国际母语日",
            "0303 国际爱耳日",
            "0308 妇女节",
            "0312 植树节",
            "0315 消费者权益日",
            "0322 世界水日",
            "0323 世界气象日",
            "0401 愚人节",
            "0407 世界卫生日",
            "0501 劳动节",
            "0504 青年节",
            "0512 护士节",
            "0519 全国助残日",
            "0531 世界无烟日",
            "0601 儿童节",
            "0626 国际禁毒日",
            "0701 建党节", //1921
            "0801 建军节", //1933
            //"0808 父亲节",
            "0909 毛泽东逝世纪念", //1976
            "0910 教师节",
            "0917 国际和平日",
            "0927 世界旅游日",
            "0928 孔子诞辰",
            "1001 国庆节",
            "1006 老人节",
            "1007 国际住房日",
            "1014 世界标准日",
            "1024 联合国日",
            "1112 孙中山诞辰纪念",
            "1210 世界人权日",
            "1220 澳门回归纪念",
            "1224 平安夜",
            "1225 圣诞节",
            "1226 毛泽东诞辰纪念" };
    //24节气
    final static String[] sTermInfo = new String[]{
            // 时节     气候
            "小寒","大寒",
            "立春","雨水",
            "惊蛰","春分",
            "清明","谷雨",
            "立夏","小满",
            "芒种","夏至",
            "小暑","大暑",
            "立秋","处暑",
            "白露","秋分",
            "寒露","霜降",
            "立冬","小雪",
            "大雪","冬至"
    };
    final static String[] constellations = new String[]{
            "摩蝎座:12.22—01.19","水瓶座:01.20—02.18","双鱼座:02.19—03.20","白羊座:03.21—04.19",
            "金牛座:04.20—05.20","双子座:05.21—06.20","巨蟹座:06.21—07.21","狮子座:07.22—08.22",
            "处女座:08.23—09.22","天秤座:09.23—10.22","天蝎座:10.23—11.21","射手座:11.22—12.21"
    };

    final static String[] yi_string = new String[]{
            "出行.上任.会友.上书.见工","除服.疗病.出行.拆卸.入宅",
            "祈福.祭祀.结亲.开市.交易","祭祀.修填.涂泥.余事勿取",
            "交易.立券.会友.签约.纳畜","祈福.祭祀.求子.结婚.立约",
            "求医.赴考.祭祀.余事勿取","经营.交易.求官.纳畜.动土",
            "祈福.入学.开市.求医.成服","祭祀.求财.签约.嫁娶.订盟",
            "疗病.结婚.交易.入仓.求职","祭祀.交易.收财.安葬"
    };
    final static String[] ji_string = new String[]{
            "动土.开仓.嫁娶.纳采","求官.上任.开张.搬家.探病",
            "服药.求医.栽种.动土.迁移","移徙.入宅.嫁娶.开市.安葬",
            "种植.置业.卖田.掘井.造船","开市.交易.搬家.远行",
            "动土.出行.移徙.开市.修造","登高.行船.安床.入宅.博彩",
            "词讼.安门.移徙","开市.安床.安葬.入宅.破土",
            "安葬.动土.针灸","宴会.安床.出行.嫁娶.移徙"
    };
    final static String[][] jcName = new String[][]{
            {"建","除","满","平","定","执","破","危","成","收","开","闭"},
            {"闭","建","除","满","平","定","执","破","危","成","收","开"},
            {"开","闭","建","除","满","平","定","执","破","危","成","收"},
            {"收","开","闭","建","除","满","平","定","执","破","危","成"},
            {"成","收","开","闭","建","除","满","平","定","执","破","危"},
            {"危","成","收","开","闭","建","除","满","平","定","执","破"},
            {"破","危","成","收","开","闭","建","除","满","平","定","执"},
            {"执","破","危","成","收","开","闭","建","除","满","平","定"},
            {"定","执","破","危","成","收","开","闭","建","除","满","平"},
            {"平","定","执","破","危","成","收","开","闭","建","除","满"},
            {"满","平","定","执","破","危","成","收","开","闭","建","除"},
            {"除","满","平","定","执","破","危","成","收","开","闭","建"}
    };
    final static String[] Gan = new String[] { "甲", "乙", "丙", "丁", "戊", "己", "庚",
            "辛", "壬", "癸" };
    final static String[] Zhi = new String[] { "子", "丑", "寅", "卯", "辰", "巳", "午",
            "未", "申", "酉", "戌", "亥" };

    public static int[] getDateArray() {
        long rawTime = System.currentTimeMillis();
        Date d = new Date(rawTime);
        SimpleDateFormat format;
        String[] patterns = {
                "yyyy",
                "MM",
                "dd"
        };
        int[] result = new int[3];
        int index = 0;
        for (String pattern : patterns) {
            format = new SimpleDateFormat(pattern, Locale.getDefault());
            result[index] = Integer.parseInt(format.format(d));
            index++;
        }
        return result;
    }

    // ====== 传回农历 y年的总天数 1900--2100
    public static int yearDays(int y) {
        int i, sum = 348;
        for (i = 0x8000; i > 0x8; i >>= 1) {
            if ((lunarInfo[y - 1900] & i) != 0)
                sum += 1;
        }
        return (sum + leapDays(y));
    }

    // ====== 传回农历 y年闰月的天数
    public static int leapDays(int y) {
        if (leapMonth(y) != 0) {
            if ((lunarInfo[y - 1899] & 0xf) != 0)
                return 30;
            else
                return 29;
        } else
            return 0;
    }

    // ====== 传回农历 y年闰哪个月 1-12 , 没闰传回 0
    public static int leapMonth(int y) {
        long var = lunarInfo[y - 1900] & 0xf;
        return (int)(var == 0xf ? 0 : var);
    }

    // ====== 传回农历 y年m月的总天数
    public static int monthDays(int y, int m) {
        if ((lunarInfo[y - 1900] & (0x10000 >> m)) == 0)
            return 29;
        else
            return 30;
    }

    // ====== 传回农历 y年的生肖
    public String animalsYear(int year) {
        final String[] Animals = new String[] { "鼠", "牛", "虎", "兔", "龙", "蛇",
                "马", "羊", "猴", "鸡", "狗", "猪" };
        return Animals[(year - 4) % 12]+"年";
    }

    // ====== 传入 月日的offset 传回干支, 0=甲子
    private static String cyclicalm(int num) {
        return (Gan[num % 10] + Zhi[num % 12]);
    }

    // ====== 传入 offset 传回干支, 0=甲子
    public static String cyclical(int year, int month, int day) {
        int num = year - 1900 + 36;
        //立春日期
        int term2 = sTerm(year, 2);
        if (month <= 2 && (month != 2 || day < term2)) {
            num -= 1;
        }
        return (cyclicalm(num));
    }

    public static String getChinaDayString(int day) {//将农历day日格式化成农历表示的字符串
        String[] chineseTen = { "初", "十", "廿", "卅" };
        String[] chineseDay = { "一", "二", "三", "四", "五", "六", "七",
                "八", "九", "十"};
        String var;
        if(day!=20 && day != 30){
            var = chineseTen[(day-1)/10]+ chineseDay[(day-1)%10];
        }else if(day !=20){
            var = chineseTen[day/10]+"十";
        }else{
            var = "二十";
        }
        return var;
    }
    /**
     * 计算公历nY年nM月nD日和bY年bM月bD日渐相差多少天
     */
    public static int getDaysOfTwoDate(int bY, int bM, int bD, int nY, int nM, int nD){
        Date baseDate = null;
        Date nowaday = null;
        try {
            baseDate = chineseDateFormat.parse(bY+"年"+bM+"月"+bD+"日");
            nowaday = chineseDateFormat.parse(nY+"年"+nM+"月"+nD+"日");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // 求出相差的天数
        return (int) ((nowaday.getTime() - baseDate.getTime()) / 86400000L);
    }
    /**
     * 从农历日期转换成公历日期
     *
     * @param lunYear 农历年
     * @param lunMonth 农历月
     * @param lunDay 农历日
     * @param isLeap 当前年月是否是闰月
     * @return 对应的公历日期
     */
    public static Calendar getSunDate(int lunYear, int lunMonth, int lunDay, boolean isLeap){
        //公历1900年1月31日为1900年正月初一
        int years = lunYear -1900;
        int days=0;
        for(int i=0;i<years;i++){
            days += yearDays(1900+i);//农历某年总天数
        }
        for(int i=1;i<lunMonth;i++){
            days +=monthDays(lunYear,i);
        }
        if(leapMonth(lunYear)!=0 && lunMonth>leapMonth(lunYear)){
            days += leapDays(lunYear);//lunYear年闰月天数
        }
        if(isLeap){
            days +=monthDays(lunYear,lunMonth);//lunYear年lunMonth月 闰月
        }
        days +=lunDay;
        days = days-1;
        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.YEAR, 1900);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH,31);
        cal.add(Calendar.DATE, days);
	    /*
	    Date date=cal.getTime();
	    int year_c = cal.get(Calendar.YEAR);
	    int month_c = cal.get(Calendar.MONTH)+1;
	    int day_c = cal.get(Calendar.DAY_OF_MONTH);
	    */
        return cal;
    }

    /**
     * 获取农历日期
     * @param date 公历年月日组成的数组（可通过getDateArray()获取）
     * @return 农历日期（字符串）
     */
    public static String getLunarString(int[] date){

        int year = date[0];
        int month = date[1];
        int day = date[2];

        int var = getLunarDateINT(year,month,day);
        int lYear = var /10000;
        int lMonth = (var%10000) /100;
        int lDay = var - lYear*10000 - lMonth*100;
        String lunY = cyclical(year,month,day)+"年";
        String lunM="";
        int testMonth = getSunDate(lYear,lMonth,lDay,false).get(Calendar.MONTH)+1;
        if(testMonth!=month){
            lunM = "闰";
        }
        lunM += chineseNumber[(lMonth-1)%12]+"月";
        //int leap = leapMonth(lYear);
        String lunD = getChinaDayString(lDay);
        int animalsYear = 0;
        int term2 = sTerm(year, 2);//立春
        if(month>2 ||(month==2&& day>=term2)){
            animalsYear = year;
        }else{
            animalsYear = year-1;
        }
        String Festival = getFestival(year,month,day);
        //return lunY+"-"+lunM+"-"+lunD+"-"+animalsYear(animalsYear);
//        return "农历 "+lunM+lunD+" "+Festival;
        return lunM+lunD;
    }
    /**
     * 将公历year年month月day日转换成农历
     * @return 返回格式为20140506（int）
     * */
    public static int getLunarDateINT(int year, int month, int day){
        int iYear,LYear,LMonth,LDay, daysOfYear = 0;
        // 求出和1900年1月31日相差的天数
        //year =1908;
        //month = 3;
        //day =3;
        int offset = getDaysOfTwoDate(1900,1,31,year,month,day);
        //Log.i("--ss--","公历:"+year+"-"+month+"-"+day+":"+offset);
        // 用offset减去每农历年的天数
        // 计算当天是农历第几天
        // i最终结果是农历的年份
        // offset是当年的第几天
        for (iYear = 1900; iYear < 2100 && offset >0; iYear++) {
            daysOfYear = yearDays(iYear);
            offset -= daysOfYear;
            //Log.i("--ss--","农历:"+iYear+":"+daysOfYear+"/"+offset);
        }

        if (offset < 0) {
            offset += daysOfYear;
            iYear--;
            //Log.i("--ss--","农历:"+iYear+":"+daysOfYear+"/"+offset);
        }
        // 农历年份
        LYear = iYear;

        leapMonth = leapMonth(iYear); // 闰哪个月,1-12
        boolean leap = false;

        // 用当年的天数offset,逐个减去每月（农历）的天数，求出当天是本月的第几天
        int iMonth, daysOfMonth = 0;
        for (iMonth = 1; iMonth < 13 && offset >0; iMonth++) {
            // 闰月
            if (leapMonth > 0 && iMonth == (leapMonth + 1) && !leap) {
                --iMonth;
                leap = true;
                daysOfMonth = leapDays(iYear);
            } else{
                daysOfMonth = monthDays(iYear, iMonth);
            }
            // 解除闰月
            if (leap && iMonth == (leapMonth + 1)) leap = false;

            offset -= daysOfMonth;
            //Log.i("--ss--","农历:"+iYear+"-"+iMonth+":"+daysOfMonth+"/"+offset);
        }
        // offset为0时，并且刚才计算的月份是闰月，要校正
        if (offset == 0 && leapMonth > 0 && iMonth == leapMonth + 1) {
            if (leap) {
                leap = false;
            } else {
                leap = true;
                --iMonth;
            }
        }
        // offset小于0时，也要校正
        if (offset < 0) {
            offset += daysOfMonth;
            --iMonth;
            //Log.i("--ss--","农历:"+iYear+"-"+iMonth+":"+daysOfMonth+"/"+offset);
        }
        LMonth = iMonth;
        LDay = offset + 1;
        //Log.i("--ss--","农历:"+LYear+"-"+LMonth+"-"+LDay);
        return LYear * 10000 + LMonth *100 + LDay;
    }
    public static String getFestival(int year, int month, int day){//获取公历对应的节假日或二十四节气

        //农历节假日
        int var = getLunarDateINT(year, month, day);
        int lun_year = var /10000;
        int lun_month = (var%10000) /100;
        int lun_Day = var - lun_year*10000 - lun_month*100;
        for (String s : lunarHoliday) {
            // 返回农历节假日名称
            String ld = s.split(" ")[0]; // 节假日的日期
            String ldv = s.split(" ")[1]; // 节假日的名称
            String lmonth_v = lun_month + "";
            String lday_v = lun_Day + "";
            String lmd;
            if (month < 10) {
                lmonth_v = "0" + lun_month;
            }
            if (day < 10) {
                lday_v = "0" + lun_Day;
            }
            lmd = lmonth_v + lday_v;
            if (ld.trim().equals(lmd.trim())) {
                return ldv;
            }
        }

        //公历节假日
        for (int i = 0; i < solarHoliday.length; i++) {
            if(i==solarHoliday.length && year<1893 //1893年前没有此节日
                    || i+3 == solarHoliday.length && year<1999
                    || i+6 == solarHoliday.length && year<1942
                    || i+10 == solarHoliday.length && year<1949
                    || i == 19 && year<1921
                    || i == 20 && year<1933
                    || i == 22 && year<1976){
                break;
            }
            // 返回公历节假日名称
            String sd = solarHoliday[i].split(" ")[0]; // 节假日的日期
            String sdv = solarHoliday[i].split(" ")[1]; // 节假日的名称
            String smonth_v = month + "";
            String sday_v = day + "";
            String smd;
            if (month < 10) {
                smonth_v = "0" + month;
            }
            if (day < 10) {
                sday_v = "0" + day;
            }
            smd = smonth_v + sday_v;
            if (sd.trim().equals(smd.trim())) {
                return sdv;
            }
        }


        int b = getDateOfSolarTerms(year,month);
        if (day == b /100){
            return sTermInfo[(month-1)*2];
        } else if(day == b%100){
            return sTermInfo[(month-1)*2+1];
        }
        return "";
    }

    /**
     * 传出y年m月d日对应的农历
     *
     * @param isday 这个参数为false---日期为节假日时，阴历日期就返回节假日 ，true---不管日期是否为节假日依然返回这天对应的阴历日期
     * @param year_log 农历年与1864的相差数
     * @param month_log 从1900年1月31日以来,闰月数
     * @param day_log 与1900年1月31日相差的天数,再加40
     * @return 传出y年m月d日对应的农历
     */
    public String getLunarDate(int year_log, int month_log, int day_log,
                               boolean isday) {
        int var = getLunarDateINT(year_log,month_log,day_log);
        // 农历年份
        year = var /10000;
        setYear(year); // 设置公历对应的农历年份

        //农历月份
        month = (var%10000) /100;
        setLunarMonth(chineseNumber[(month-1)%12] + "月"); // 设置对应的阴历月份
        day = var - year*10000 - month*100;
        if (!isday) {
            String Festival = getFestival(year_log,month_log,day_log);
            if(Festival.length()>1){
                return Festival;
            }
        }
        if (day == 1)
            return chineseNumber[(month-1)%12] + "月";
        else
            return getChinaDayString(day);

    }

    @NonNull
    public String toString() {
        if (chineseNumber[(month-1)%12].equals("正") && getChinaDayString(day).equals("初一"))
            return "农历" + year + "年";
        else if (getChinaDayString(day).equals("初一"))
            return chineseNumber[(month-1)%12] + "月";
        else
            return getChinaDayString(day);
        // return year + "年" + (leap ? "闰" : "") + chineseNumber[month - 1] +
        // "月" + getChinaDayString(day);
    }

    /*
     * public static void main(String[] args) { System.out.println(new
     * LunarCalendar().getLunarDate(2012, 1, 23)); }
     */

    public int getLeapMonth() {
        return leapMonth;
    }

    public void setLeapMonth(int leapMonth) {
        LunarCalender.leapMonth = leapMonth;
    }

    /**
     * 得到当前日期对应的阴历月份
     *
     * @return 当前日期对应的阴历月份
     */
    public String getLunarMonth() {
        return lunarMonth;
    }

    public void setLunarMonth(String lunarMonth) {
        this.lunarMonth = lunarMonth;
    }

    /**
     * 得到当前年对应的农历年份
     *
     * @return 当前年对应的农历年份
     */
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
    public String getConstellation(int month, int day){//计算星座
        int date = month*100 + day;
        String constellation;
        if(date>=120 && date <=218){
            constellation = constellations[1];
        }else if(date>=219 && date<=320){
            constellation = constellations[2];
        }else if(date>=321 && date<=419){
            constellation = constellations[3];
        }else if(date>=420 && date<=520){
            constellation = constellations[4];
        }else if(date>=521 && date<=620){
            constellation = constellations[5];
        }else if(date>=621 && date<=721){
            constellation = constellations[6];
        }else if(date>=722 && date<=822){
            constellation = constellations[7];
        }else if(date>=823 && date<=922){
            constellation = constellations[8];
        }else if(date>=923 && date<=1022){
            constellation = constellations[9];
        }else if(date>=1023 && date<=1121){
            constellation = constellations[10];
        }else if(date>=1122 && date<=1221){
            constellation = constellations[11];
        }else{
            constellation = constellations[0];
        }
        return constellation;
    }

    private static int sTerm(int y, int n) {
        int[]  sTermInfo = new int[]{0,21208,42467,63836,85337,107014,
                128867,150921,173149,195551,218072,240693,
                263343,285989,308563,331033,353350,375494,
                397447,419210,440795,462224,483532,504758};
        Calendar cal = Calendar.getInstance();
        cal.set(1900, 0, 6, 2, 5, 0);
        long temp = cal.getTime().getTime();
        cal.setTime(new Date( (long) ((31556925974.7 * (y - 1900) + sTermInfo[n] * 60000L) + temp)));
        return cal.get(Calendar.DAY_OF_MONTH);
    }
    public static int getDateOfSolarTerms(int year, int month){
        int a = sTerm(year,(month-1)*2);
        int b = sTerm(year,(month-1)*2 +1);
        return a*100+b;
        //return 0;
    }
    public String jcrt(String d) {
        String jcrjxt="";
        String yj0 = "宜:\t",yj1 = "忌:\t",br = "-";
        //String yj0 = "",yj1 = "",br = "-";
        if (d.equals("建")) jcrjxt = yj0 + yi_string[0] + br + yj1 + ji_string[0];
        if (d.equals("除")) jcrjxt = yj0 + yi_string[1] + br + yj1 + ji_string[1];
        if (d.equals("满")) jcrjxt = yj0 + yi_string[2] + br + yj1 + ji_string[2];
        if (d.equals("平")) jcrjxt = yj0 + yi_string[3] + br + yj1 + ji_string[3];
        if (d.equals("定")) jcrjxt = yj0 + yi_string[4] + br + yj1 + ji_string[4];
        if (d.equals("执")) jcrjxt = yj0 + yi_string[5] + br + yj1 + ji_string[5];
        if (d.equals("破")) jcrjxt = yj0 + yi_string[6] + br + yj1 + ji_string[6];
        if (d.equals("危")) jcrjxt = yj0 + yi_string[7] + br + yj1 + ji_string[7];
        if (d.equals("成")) jcrjxt = yj0 + yi_string[8] + br + yj1 + ji_string[8];
        if (d.equals("收")) jcrjxt = yj0 + yi_string[9] + br + yj1 + ji_string[9];
        if (d.equals("开")) jcrjxt = yj0 + yi_string[10] + br + yj1 + ji_string[10];
        if (d.equals("闭")) jcrjxt = yj0 + yi_string[11] + br + yj1 + ji_string[11];

        return jcrjxt;
    }

    /* num_y%12, num_m%12, num_d%12, num_y%10, num_d%10
     * m:农历月份 1---12
     * dt：农历日
     * */
    public String CalConv2(int yy, int mm, int dd, int y, int d, int m, int dt) {
        String dy = d + "" + dd;

        if ((yy == 0 && dd == 6) || (yy == 6 && dd == 0) || (yy == 1 && dd == 7) ||
                (yy == 7 && dd == 1) || (yy == 2 && dd == 8) || (yy == 8 && dd == 2) ||
                (yy == 3 && dd == 9) || (yy == 9 && dd == 3) || (yy == 4 && dd == 10) ||
                (yy == 10 && dd == 4) || (yy == 5 && dd == 11) || (yy == 11 && dd == 5)) {
            /*
             * 地支共有六对相冲，俗称“六冲”，即：子午相冲、丑未相冲、寅申相冲、卯酉相冲、辰戌相冲、巳亥相冲。
             * 如当年是甲子年，子为太岁，与子相冲者是午，假如当日是午日，则为岁破，其余的以此类推，即丑年的岁破为未日，
             * 寅年的岁破为申日，卯年的岁破为酉日，辰年的岁破为戌日，巳年的岁破为亥日，午年的岁破为子日，未年的岁破为丑日，
             * 申年的岁破为寅日，酉年的岁破为卯日，戌年的岁破为辰日，亥年的岁破为巳日。
             * */
            return "日值岁破 大事不宜";
        }
        else if ((mm == 0 && dd == 6) || (mm == 6 && dd == 0) || (mm == 1 && dd == 7) || (mm == 7 && dd == 1) ||
                (mm == 2 && dd == 8) || (mm == 8 && dd == 2) || (mm == 3 && dd == 9) || (mm == 9 && dd == 3) ||
                (mm == 4 && dd == 10) || (mm == 10 && dd == 4) || (mm == 5 && dd == 11) || (mm == 11 && dd == 5)) {
            return "日值月破 大事不宜";
        }
        else if ((y == 0 && dy.equals("911")) || (y == 1 && dy.equals("55")) || (y == 2 && dy.equals("111")) ||
                (y == 3 && dy.equals("75")) || (y == 4 && dy.equals("311")) || (y == 5 && dy.equals("95")) ||
                (y == 6 && dy.equals("511"))|| (y == 7 && dy.equals("15")) || (y == 8 && dy.equals("711")) || (y == 9 && dy.equals("35"))) {
            return "日值上朔 大事不宜";
        }
        else if ((m == 1 && dt == 13) || (m == 2 && dt == 11) || (m == 3 && dt == 9) || (m == 4 && dt == 7) ||
                (m == 5 && dt == 5) || (m == 6 && dt == 3) || (m == 7 && dt == 1) || (m == 7 && dt == 29) ||
                (m == 8 && dt == 27) || (m == 9 && dt == 25) || (m == 10 && dt == 23) || (m == 11 && dt == 21) ||
                (m == 12 && dt == 19)) {
            /*
             * 杨公十三忌   以农历正月十三日始，以后每月提前两天为百事禁忌日
             * */
            return "日值杨公十三忌 大事不宜";
            //}else if(var == getSiLi(m,dt)){
            //return "日值四离  大事勿用";
        }
        else {
            return null;
        }
    }
    public int getDaysOfMonth(int year, int month){//返回公历year年month月的当月总天数
        if(month == 2){
            return(((year%4 == 0) && (year%100 != 0) || (year%400 == 0))? 29: 28);
        }else if(month == 1 || month == 3 ||month == 5 || month == 7 || month == 8 ||month == 10 || month == 12){
            return 31;
        }
        return 30;
    }
    /**
     * @param year 公历年份，大于1900
     * @param month 公历月份 1--12
     * @return year年month月全月宜忌
     **/
    public ArrayList<String> getYiJi(int year, int month) {
        int num_y = -1;
        int num_m = (year - 1900) * 12 + month-1 +12;
        int num_d;
        int mLMonth=1,mLDay=1,mLun_x=0;
        int days_of_month = getDaysOfMonth(year, month);
        ArrayList<String> yiji = new ArrayList<>();

        //年柱 1900年立春后为庚子年(60进制36)
        //cyclical(year,month);
        //立春日期
        int term2 = sTerm(year, 2);

        //月柱  1900年1月小寒以前为 丙子月(60进制12)
        int firstNode = sTerm(year, (month-1)*2);//当月的24节气中的节开始日
        //cyclicalm(num_m);
        //Calendar cal = Calendar.getInstance();
        //cal.set(year, month, 1, 0, 0, 0);
        //1900/1/1与 1970/1/1 相差25567日, 1900/1/1 日柱为甲戌日(60进制10)
        int dayCyclical = getDaysOfTwoDate(1900,1,1,year,month,1)+10;
        for(int i=0;i<days_of_month;i++){
            if(mLDay > mLun_x){
                //Log.i("","mLDay > mLun_x "+mLDay+":"+mLun_x);
                int var = getLunarDateINT(year,month,i+1);
                int mLYear = var /10000;
                mLMonth = (var%10000) /100;
                mLDay = var - mLYear*10000 - mLMonth*100;
                mLun_x = (leapMonth(mLYear)!=0)?leapDays(mLYear):monthDays(mLYear, mLMonth);
                //Log.i("","mLDay > mLun_x ?"+mLDay+":"+mLun_x);
            }
            //依节气调整二月分的年柱, 以立春为界
            if(month==2 && (i+1)==term2){
                //cY = cyclicalm(year-1900 + 36);
                num_y = year - 1900 + 36;
            }
            //依节气 调整月柱, 以「节」为界
            if((i+1)==firstNode) {
                num_m =(year - 1900) * 12 + month +12 ;
                //cM = cyclicalm(num_m);
            }
            //日柱
            //cD = cyclicalm(dayCyclical + i);
            num_d = dayCyclical +i;

            mLDay++;
            //Log.i("","---num_y:"+num_y+","+num_m+","+num_d+",n_m:"+mLMonth+",n_d:"+mLDay+",mLun_x:"+mLun_x);
            String str = CalConv2(num_y%12, num_m%12, num_d%12, num_y%10, num_d%10, mLMonth, mLDay-1);
            if(str == null){
                String var = jcName[num_m%12][num_d%12];
                str = jcrt(var);
                //Log.i("","---"+month+"-"+(i+1)+","+var+":"+str);
            }
            //Log.i("","---"+year+"-"+month+"-"+(i+1)+","+str);
            yiji.add(str);
        }

        return yiji;
    }
    /**
     * 公历某日宜忌
     **/
    public String getYiJi(int year, int month, int day) {
        int num_y = -1;
        int num_m = (year - 1900) * 12 + month-1 +12;
        int num_d;
        int mLMonth,mLDay;
        int days_of_month = getDaysOfMonth(year, month);

        if(day > days_of_month)
            day = days_of_month;

        //年柱 1900年立春后为庚子年(60进制36)
        //cyclical(year,month);
        //立春日期
        int term2 = sTerm(year, 2);
        int firstNode = sTerm(year, (month-1)*2);//当月的24节气中的节开始日
        int dayCyclical = getDaysOfTwoDate(1900,1,1,year,month,1)+10;

        if(month==2 && day==term2){
            //cY = cyclicalm(year-1900 + 36);//依节气调整二月分的年柱, 以立春为界
            num_y = year - 1900 + 36;
        }
        if(day==firstNode) {
            num_m =(year - 1900) * 12 + month +12 ;//依节气 调整月柱, 以「节」为界
            //cM = cyclicalm(num_m);
        }
        num_d = dayCyclical +day -1;

        int var = getLunarDateINT(year,month,day);
        int mLYear = var /10000;
        mLMonth = (var%10000) /100;
        mLDay = var - mLYear*10000 - mLMonth*100;

        //Log.i("","---num_y:"+num_y+","+num_m+","+num_d+",n_m:"+mLMonth+",n_d:"+mLDay+",mLun_x:"+mLun_x);
        String str = CalConv2(num_y%12, num_m%12, num_d%12, num_y%10, num_d%10, mLMonth, mLDay);
        if(str == null){
            str = jcrt(jcName[num_m%12][num_d%12]);
            //Log.i("","---"+month+"-"+(i+1)+","+var+":"+str);
        }
        //Log.i("","---"+year+"-"+month+"-"+(i+1)+","+str);
        return str;
    }
    /**
     * 计算距离1900年12月31日days天后的日期
     **/
    public int getDateFromBaseDate(int days){
        int year,month,day;
        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.YEAR, 1900);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH,31);
        cal.add(Calendar.DATE, days);
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH)+1;
        day = cal.get(Calendar.DAY_OF_MONTH);

        return 10000*year+100*month+day;
    }

}
