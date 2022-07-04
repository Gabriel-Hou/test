package com.hsbc.test.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class BaiDuRequestService {
    private static final String SAVE_FILE_PATH = "/resources";
    private static final String PATH_FLAG = "/";

    public static ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(Integer.MAX_VALUE);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("baidu");
        executor.setKeepAliveSeconds(1);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    public static void requestHandler(String path) {
        FileOutputStream out;
        InputStream is;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://www.baidu.com/s?wd=HSBC").openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows XP; DigExt)");
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            is = conn.getInputStream();

            String uuid = UUID.randomUUID().toString().replace("-", "");
            String fileName = path + PATH_FLAG + conn.getResponseCode() + "_" + uuid + ".html";

            out = new FileOutputStream(fileName);
            int a = 0;
            while ((a = is.read()) != -1) {
                out.write(a);
                log.info(">>> write html >>> ");
            }

            is.close();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            log.error("hit error when write html: ", ex.fillInStackTrace());

        }
    }

    public static void main(String[] args) {
        // set the value of request (N)
        int requestNum = 100;
        File folder = new File(SAVE_FILE_PATH);
        if (!folder.exists() && !folder.isDirectory()) {
            folder.mkdirs();
        }
        LocalDateTime now = LocalDateTime.now();
        String saveFilePath = SAVE_FILE_PATH + PATH_FLAG + now.toInstant(ZoneOffset.of("+8")).toEpochMilli();
        folder = new File(saveFilePath);
        if (!folder.exists() && !folder.isDirectory()) {
            folder.mkdirs();
        }

        //execute
        ThreadPoolTaskExecutor threadPoolTaskExecutor = taskExecutor();
        for (int i = 0; i < requestNum; i++) {
            threadPoolTaskExecutor.execute(() -> requestHandler(saveFilePath));
        }
    }
}
