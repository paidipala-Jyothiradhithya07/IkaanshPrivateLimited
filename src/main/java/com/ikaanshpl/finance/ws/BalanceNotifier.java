package com.ikaanshpl.finance.ws;

import java.time.Instant;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ikaanshpl.finance.dto.Dtos.BalanceUpdate;

@Component
public class BalanceNotifier {
	 private final SimpMessagingTemplate template;
	 

	    public BalanceNotifier(SimpMessagingTemplate template) {
		super();
		this.template = template;
	}


		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	    public void onBalanceChanged(BalanceChangedEvent e) {
	        template.convertAndSendToUser(String.valueOf(e.userId()), "/queue/balance",
	                new BalanceUpdate(e.accountId(), e.balance(), Instant.now()));
	    }
}
