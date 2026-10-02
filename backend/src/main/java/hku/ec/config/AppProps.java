package hku.ec.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

/** 从 application.yml 的 app.* 读入（缓冲、开放时段、跨域来源） */
@ConfigurationProperties(prefix = "app")
public class AppProps {

    /** 换场缓冲分钟数：相邻两个占用段之间不足这个长度就不算可用 */
    private int bufferMinutes = 10;

    private LocalTime openingStart = LocalTime.of(8, 0);
    private LocalTime openingEnd = LocalTime.of(22, 0);

    private String corsAllowedOrigin = "http://localhost:5173";

    public int getBufferMinutes() { return bufferMinutes; }
    public void setBufferMinutes(int bufferMinutes) { this.bufferMinutes = bufferMinutes; }

    public LocalTime getOpeningStart() { return openingStart; }
    public void setOpeningStart(LocalTime openingStart) { this.openingStart = openingStart; }

    public LocalTime getOpeningEnd() { return openingEnd; }
    public void setOpeningEnd(LocalTime openingEnd) { this.openingEnd = openingEnd; }

    public String getCorsAllowedOrigin() { return corsAllowedOrigin; }
    public void setCorsAllowedOrigin(String corsAllowedOrigin) { this.corsAllowedOrigin = corsAllowedOrigin; }
}
