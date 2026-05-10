package vr46.switchbotctrlmcppublic.logging;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MyLogger {
    public final Logger logger;
    public final String className;
    public final String classVersion;
    public MyLogger(String className, String classVersion){
        this.className = className;
        this.classVersion = classVersion;
        this.logger = LoggerFactory.getLogger(this.className);
    }

    // ANSIエスケープシーケンスの定義
    public final String ANSI_RESET = "\u001B[0m";
    public final String ANSI_BLACK = "\u001B[30m";
    public final String ANSI_RED = "\u001B[31m";
    public final String ANSI_GREEN = "\u001B[32m";
    public final String ANSI_YELLOW = "\u001B[33m";
    public final String ANSI_BLUE = "\u001B[34m";
    public final String ANSI_PURPLE = "\u001B[35m";
    public final String ANSI_CYAN = "\u001B[36m";
    public final String ANSI_WHITE = "\u001B[37m";

    public void start(){
        logger.info("START - Ver " + this.classVersion + " -------------------");
    }

    public void run(){
        logger.info("RUN - Ver " + this.classVersion + " -------------------");
    }
    public void run(int period){
        String periodStr = "";
        if(period < 300) {
            periodStr = period + " sec";
        }
        if(period >= 300 && period < 3600) {
            double period2 = ((double) Math.round((float) period / 60 * 10) / 10);
            periodStr = period2 + " min";
        }
        if(period >= 3600) {
            double period2 = ((double) Math.round((float) period / 60 / 60 * 10) / 10);
            periodStr = period2 + " hour";
        }

        logger.info("RUN - Ver " + this.classVersion + " - period " + periodStr + " ------------------");
    }

    public void nextRun(int period){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, period);
        Date netRunDate = calendar.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        logger.info("NEXT RUN : " + formatter.format(netRunDate));
    }

    public void finish(){
        logger.info("FINISH -------------------");
    }

    public void finish(int period){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, period);
        Date netRunDate = calendar.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        logger.info("FINISH - NEXT RUN : " + formatter.format(netRunDate) + "-------------------");
    }

    public void split(){
        logger.info("---------------------------------------");
    }

    public void info(String message){
        logger.info(message);
    }
    public void info1(String message){
        logger.info(" " + message);
    }
    public void info2(String message){
        logger.info("  " + message);
    }
    public void info3(String message){
        logger.info("   " + message);
    }

    public void info_CYAN(String message){
        logger.info(ANSI_CYAN + message + ANSI_RESET);
    }
    public void info1_CYAN(String message){
        logger.info(" " + ANSI_CYAN + message + ANSI_RESET);
    }
    public void info2_CYAN(String message){
        logger.info("  " + ANSI_CYAN + message + ANSI_RESET);
    }
    public void info3_CYAN(String message){
        logger.info("   " + ANSI_CYAN + message + ANSI_RESET);
    }

    public void info_GREEN(String message){
        logger.info(ANSI_GREEN + message + ANSI_RESET);
    }
    public void info1_GREEN(String message){
        logger.info(" " + ANSI_GREEN + message + ANSI_RESET);
    }
    public void info2_GREEN(String message){
        logger.info("  " + ANSI_GREEN + message + ANSI_RESET);
    }
    public void info3_GREEN(String message){
        logger.info("   " + ANSI_GREEN + message + ANSI_RESET);
    }

    public void info_BLUE(String message){
        logger.info(ANSI_BLUE + message + ANSI_RESET);
    }
    public void info1_BLUE(String message){
        logger.info(" " + ANSI_BLUE + message + ANSI_RESET);
    }
    public void info2_BLUE(String message){
        logger.info("  " + ANSI_BLUE + message + ANSI_RESET);
    }
    public void info3_BLUE(String message){
        logger.info("   " + ANSI_BLUE + message + ANSI_RESET);
    }

    public void info_YELLOW(String message){
        logger.info(ANSI_YELLOW + message + ANSI_RESET);
    }
    public void info1_YELLOW(String message){
        logger.info(" " + ANSI_YELLOW + message + ANSI_RESET);
    }
    public void info2_YELLOW(String message){
        logger.info("  " + ANSI_YELLOW + message + ANSI_RESET);
    }
    public void info3_YELLOW(String message){
        logger.info("   " + ANSI_YELLOW + message + ANSI_RESET);
    }

    public void info_RED(String message){
        logger.info(ANSI_RED + message + ANSI_RESET);
    }
    public void info1_RED(String message){
        logger.info(" " + ANSI_RED + message + ANSI_RESET);
    }
    public void info2_RED(String message){
        logger.info("  " + ANSI_RED + message + ANSI_RESET);
    }
    public void info3_RED(String message){
        logger.info("   " + ANSI_RED + message + ANSI_RESET);
    }
    public void error(String message){
        logger.error(message);
    }
    public void error_RED(String message){
        logger.error(ANSI_RED + message + ANSI_RESET);
    }
    public void error_YELLOW(String message){
        logger.error(ANSI_YELLOW + message + ANSI_RESET);
    }

    public static String getNextRunDate(int period)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, period);
        Date netRunDate = calendar.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return formatter.format(netRunDate);
    }
}