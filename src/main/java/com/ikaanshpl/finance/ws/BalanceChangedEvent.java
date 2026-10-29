package com.ikaanshpl.finance.ws;

import java.math.BigDecimal;

public record BalanceChangedEvent(Long userId, Long accountId, BigDecimal balance) {

}
