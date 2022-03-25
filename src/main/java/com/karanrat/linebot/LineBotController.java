package com.karanrat.linebot;

import com.google.common.io.ByteStreams;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.LocationMessageContent;
import com.linecorp.bot.model.event.message.StickerMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.*;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@LineMessageHandler
public class LineBotController {
    @Autowired
    private LineMessagingClient lineMessagingClient;

    @EventMapping
    public void handleTextMessage(MessageEvent<TextMessageContent> event) {
        log.info(event.toString());
        TextMessageContent message = event.getMessage();
        handleTextContent(event.getReplyToken(), event, message);
    }

    @EventMapping
    public void handleStickerMessage(MessageEvent<StickerMessageContent> event) {
        log.info(event.toString());
        StickerMessageContent message = event.getMessage();
        reply(event.getReplyToken(), new StickerMessage(
                message.getPackageId(), message.getStickerId()
        ));
    }

    @EventMapping
    public void handleLocationMessage(MessageEvent<LocationMessageContent> event) {
        log.info(event.toString());
        LocationMessageContent message = event.getMessage();
        reply(event.getReplyToken(), new LocationMessage(
                (message.getTitle() == null) ? "Location replied" : message.getTitle(),
                message.getAddress(),
                message.getLatitude(),
                message.getLongitude()
        ));
    }

    @EventMapping
    public void handleImageMessage(MessageEvent<ImageMessageContent> event) {
        log.info(event.toString());
        ImageMessageContent content = event.getMessage();
        String replyToken = event.getReplyToken();

        try {
            MessageContentResponse response = lineMessagingClient.getMessageContent(content.getId()).get();
            DownloadedContent jpg = saveContent("jpg", response);
            DownloadedContent previewImage = createTempFile("jpg");

            system("convert", "-resize", "240x",
                    jpg.path.toString(),
                    previewImage.path.toString());

            reply(replyToken, new ImageMessage(jpg.getUri(), previewImage.getUri()));

        } catch (InterruptedException | ExecutionException e) {
            reply(replyToken, new TextMessage("Cannot get image: " + content));
            throw new RuntimeException(e);
        }

    }

    private void handleTextContent(String replyToken, Event event, TextMessageContent content) {
        String text = content.getText();

        log.info("Got text message from %s : %s", replyToken, text);

        switch (text) {
            case "profile": {
                String userId = event.getSource().getUserId();
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("Display name: " + profile.getDisplayName()),
                                        new TextMessage("Status message: " + profile.getStatusMessage()),
                                        new TextMessage("User ID: " + profile.getUserId())
                                ));
                            });
                }
                break;
            }


            case "order": {
                log.info("You have an order! ");
                this.replyText(replyToken, "สั่งอาหารค้าบบบบ");
            }

            case "ขนมหวาน": {
                this.reply(replyToken, Arrays.asList(
                    new TextMessage("Menu ขนมหวาน"),
                    new TextMessage("ชีสเค้ก(D1)\tราคา 39บาท\nสตรอว์เบอร์รีชีสเค้ก(D2)\tราคา 39บาท\nทีรามิสุ(D3)\tราคา 39บาท\nบราวน์ชูการ์โทสต์(D4)\tราคา 39บาท\nเค้กเรดเวลเวท(D5)\tราคา 39บาท\n"),
                
                ));
                
            }

            

            case "อาหาร": {
                this.reply(replyToken, Arrays.asList(
                    new TextMessage("MEnu อาหาร"),
                    new TextMessage("ไข่กระทะ(F1)\tราคา 40บาท\nมินิพิซซ่าแฮมชีส(F2)\tราคา 59บาท\nแซนด์วิชไก่กรอบ(F3)\tราคา 39บาท\nสลัดไข่เจียว(F4)\tราคา 35บาท\nสเต๊กหมูพันเบคอน(F5)\tราคา 69บาท\n")

                ));
                
            }

            case "กาแฟ": {
                this.reply(replyToken, Arrays.asList(
                    new TextMessage("Menu กาแฟ"),
                    new TextMessage("เอสเพรสโซ(C1)\tราคา 45บาท\nอเมริกาโน(C2)\tราคา 45บาท\nลาเต้(C3)\tราคา 45บาท\nคาปูชิโน(C4)\tราคา 45บาท\nมอคค่า(C5)\tราคา 45บาท\n")
                ));
                
            }

            case "ชานม": {
                this.reply(replyToken, Arrays.asList(
                    new TextMessage("Menu ชานม"),
                    new TextMessage("ชานมไต้หวัน(M1)\tราคา 40บาท\nมัทฉะญี่ปุ่น(M2)\tราคา 40บาท\nโกโก้(M3)\tราคา 40บาท\nชาลาวา(M4)\tราคา 40บาท\nชาชีส(M5)\tราคา 40บาท\nชาเขียว(M6)\tราคา 40บาท\nชาไทย(M7)\tราคา 40บาท")
                ));
                
            }

            case "M1":{
                this.reply(replyToken,Arrays.asList(
                    new TextMessage("หวานน้อย(1)\nหวานมาก(2)\nหวานปกติ(3)")
                ));
            }

            case "M2":{
                this.reply(replyToken,Arrays.asList(
                    new TextMessage("หวานน้อย(1)\nหวานมาก(2)\nหวานปกติ(3)")
                ));
            }

            case "M3":{
                this.reply(replyToken,Arrays.asList(
                    new TextMessage("หวานน้อย(1)\nหวานมาก(2)\nหวานปกติ(3)")
                ));
            }

            case "M4":{
                this.reply(replyToken,Arrays.asList(
                    new TextMessage("หวานน้อย(1)\nหวานมาก(2)\nหวานปกติ(3)")
                ));
            }

            case "M5":{
                this.reply(replyToken,Arrays.asList(
                    new TextMessage("หวานน้อย(1)\nหวานมาก(2)\nหวานปกติ(3)")
                ));
            }

            case "M6":{
                this.reply(replyToken,Arrays.asList(
                    new TextMessage("หวานน้อย(1)\nหวานมาก(2)\nหวานปกติ(3)")
                ));
            }

            case "M7":{
                this.reply(replyToken,Arrays.asList(
                    new TextMessage("หวานน้อย(1)\nหวานมาก(2)\nหวานปกติ(3)")
                ));
            }

            case "สวัสดี": {
                this.replyText(replyToken, "สวัสดีค่ะ รับอะไรดีคะเลือกหมวดหมูตามรูปได้เลยค่ะ");
            }

            default:
                log.info("Return uncommand message %s : %s", replyToken, text);
                this.replyText(replyToken, "ขออภัย ทางเราไม่ได้มีคำสั่งนั้น");
                this.replyText(replyToken, "ไลน์บอทของทางร้านจะมีคำสั่งดังนี้: ");
                this.replyText(replyToken, "พิมพ์ 'order' : เพื่อเข้าสู่ขั้นตอนการสั่งอาหาร");
                this.replyText(replyToken, "พิมพ์ 'help' : เพื่อดูวิธีใช้งานไลน์บอท");
        }
    }

    private void handleStickerContent(String replyToken, StickerMessageContent content) {
        reply(replyToken, new StickerMessage(
                content.getPackageId(), content.getStickerId()
        ));
    }

    private void replyText(@NonNull  String replyToken, @NonNull String message) {
        if(replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken is not empty");
        }

        if(message.length() > 1000) {
            message = message.substring(0, 1000 - 2) + "...";
        }
        this.reply(replyToken, new TextMessage(message));
    }

    private void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, Collections.singletonList(message));
    }

    private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
        try {
            BotApiResponse response = lineMessagingClient.replyMessage(
                    new ReplyMessage(replyToken, messages)
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void system(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {
            Process start = processBuilder.start();
            int i = start.waitFor();
            log.info("result: {} => {}", Arrays.toString(args), i);
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent saveContent(String ext, MessageContentResponse response) {
        log.info("Content-type: {}", response);
        DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ByteStreams.copy(response.getStream(), outputStream);
            log.info("Save {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now() + "-" + UUID.randomUUID().toString() + "." + ext;
        Path tempFile = Application.downloadedContentDir.resolve(fileName);
        tempFile.toFile().deleteOnExit();
        return new DownloadedContent(tempFile, createUri("/downloaded/" + tempFile.getFileName()));

    }

    private static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(path).toUriString();
    }

    @Value
    public static class DownloadedContent {
        Path path;
        String uri;
    }
}