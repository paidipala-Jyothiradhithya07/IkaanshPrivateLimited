package com.ikaanshpl.finance.ws;

import com.ikaansh.finance.security.AuthUser;


import com.ikaansh.finance.security.BearerTokenAuthenticator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/** Authenticates the STOMP CONNECT frame with the same JWT as the REST API. Principal name = userId. */
@Component

public class StompAuthInterceptor implements ChannelInterceptor {
    private final BearerTokenAuthenticator authenticator;
    

    public StompAuthInterceptor(BearerTokenAuthenticator authenticator) {
		super();
		this.authenticator = authenticator;
	}


	@Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc != null && StompCommand.CONNECT.equals(acc.getCommand())) {
            String header = acc.getFirstNativeHeader("Authorization");
            AuthUser user = (header != null && header.startsWith("Bearer "))
                    ? authenticator.authenticate(header.substring(7)) : null;
            if (user == null) throw new MessagingException("Unauthorized");
            acc.setUser(new UsernamePasswordAuthenticationToken(String.valueOf(user.id()), null, List.of()));
        }
        return message;
    }
}
