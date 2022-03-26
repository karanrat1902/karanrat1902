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
    int ticket ;
    
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
                    new TextMessage("ชีสเค้ก(d1)\tราคา 39บาท\nสตรอว์เบอร์รีชีสเค้ก(d2)\tราคา 39บาท\nทีรามิสุ(d3)\tราคา 39บาท\nบราวน์ชูการ์โทสต์(d4)\tราคา 39บาท\nเค้กเรดเวลเวท(d5)\tราคา 39บาท\n")
                
                ));
                
            }
            
            

            case "อาหาร": {
                this.reply(replyToken, Arrays.asList(
                    new TextMessage("Menu อาหาร"),
                    new TextMessage("ไข่กระทะ(f1)\tราคา 40บาท\nมินิพิซซ่าแฮมชีส(f2)\tราคา 59บาท\nแซนด์วิชไก่กรอบ(f3)\tราคา 39บาท\nสลัดไข่เจียว(f4)\tราคา 35บาท\nสเต๊กหมูพันเบคอน(f5)\tราคา 69บาท\nพิเสษ + 10 บาท\n")

                ));
              
            }
            
            case "กาแฟ": {
                this.reply(replyToken, Arrays.asList(
                    new TextMessage("Menu กาแฟ"),
                    new TextMessage("เอสเพรสโซ(c1)\tราคา 45บาท\nอเมริกาโน(c2)\tราคา 45บาท\nลาเต้(c3)\tราคา 45บาท\nคาปูชิโน(c4)\tราคา 45บาท\nมอคค่า(c5)\tราคา 45บาท\n")
                ));
                
            }

            case "ชานม": {
                this.reply(replyToken, Arrays.asList(
                    new TextMessage("Menu ชานม"),
                    new TextMessage("ชานมไต้หวัน(m1)\tราคา 40บาท\nมัทฉะญี่ปุ่น(m2)\tราคา 40บาท\nโกโก้(m3)\tราคา 40บาท\nชาลาวา(m4)\tราคา 40บาท\nชาชีส(m5)\tราคา 40บาท\nชาเขียว(m6)\tราคา 40บาท\nชาไทย(m7)\tราคา 40บาท")
                ));
                
            }

            case "สั่ง m11": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาใต้หวัน หวานน้อย \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m12": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาใต้หวัน หวานปกติ \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m13": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาใต้หวัน หวานมาก \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }
            case "สั่ง m21": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า มัทฉะญี่ปุ่น หวานน้อย \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m22": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า มัทฉะญี่ปุ่น หวานปกติ \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m23": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า มัทฉะญี่ปุ่น หวานมาก \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m31": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า โกโก้ หวานน้อย \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m32": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า โกโก้ หวานปกติ \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m33": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า โกโก้ หวานมาก \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m41": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาลาวา หวานน้อย \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m42": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาลาวา หวานปกติ \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m43": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาลาวา หวานมาก \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }
            case "สั่ง m51": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาชีส หวานน้อย \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m52": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาชีส หวานปกติ \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m53": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาชีส หวานมาก \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }
            case "สั่ง m61": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาเขียว หวานน้อย \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m62": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาเขียว หวานปกติ \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m63": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาเขียว หวานมาก \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m71": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาไทย หวานน้อย \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m72": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาไทย หวานปกติ \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง m73": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชาไทย หวานมาก \n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c11": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า เอสเพรสโซ หวานน้อย \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c12": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า เอสเพรสโซ หวานปกติ \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c13": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า เอสเพรสโซ หวานมาก \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c21": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า อเมริกาโน หวานน้อย \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c22": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า อเมริกาโน หวานปกติ \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c23": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า อเมริกาโน หวานมาก \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c31": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ลาเต้ หวานน้อย \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c32": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ลาเต้ หวานปกติ \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c33": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ลาเต้ หวานมาก \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c41": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า คาปูชิโน หวานน้อย \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c42": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า คาปูชิโน หวานปกติ \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c43": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า คาปูชิโน หวานมาก \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c51": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า มอคค่า หวานน้อย \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c52": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า มอคค่า หวานปกติ \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง c53": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า มอคค่า หวานมาก \n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }
            
            case "สั่ง d1": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ชีสเค้ก\n\nรายการ 39 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง d2": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า สตรอว์เบอร์รีชีสเค้ก\n\nรายการ 39 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง d3": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ทีรามิสุ\n\nรายการ 39 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง d4": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า บราวน์ชูการ์โทสต์\n\nรายการ 39 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง d5": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า เค้กเรดเวลเวท\n\nรายการ 39 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }
            
            case "สั่ง f1 ธรรมดา": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ไข่กระทะ ธรรมดา\n\nรายการ 40 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง f1 พิเศษ": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า ไข่กระทะ พิเศษ \n\nรายการ 50 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง f2 ธรรมดา": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า มินิพิซซ่าแฮมชีส\n\nรายการ 59 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง f2 พิเศษ": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า มินิพิซซ่าแฮมชีส พิเศษ\n\nรายการ 69 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }
            case "สั่ง f3 ธรรมดา": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า แซนด์วิชไก่กรอบ\n\nรายการ 39 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง f3 พิเศษ": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า แซนด์วิชไก่กรอบ พิเศษ\n\nรายการ 49 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง f4 ": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า สลัดไข่เจียว\n\nรายการ 35 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง f4 พิเศษ": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า สลัดไข่เจียว พิเศษ\n\nรายการ 45 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง f5": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า สเต๊กหมูพันเบคอน\n\nรายการ 69 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
            }

            case "สั่ง f5 พิเศษ": {
                String userId = event.getSource().getUserId();
                ticket = ticket+1;
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("คุณ "+profile.getDisplayName()+"\nคิวของคุณคือ"+ticket+"\nรายการสินค้า สเต๊กหมูพันเบคอน\n\nรายการ 79 บาท\n\nกรุณารอฟังการเรียกคิว อาหารจะเสร็จภายใน 15 นาที📢📢📢")

                                ));
                            });
                }
                break;
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