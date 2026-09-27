package com.movie.mail_service.service;

import com.movie.mail_service.event.EmailVerificationMailEvent;

public interface EmailSender {
    void send(EmailVerificationMailEvent event);
}
