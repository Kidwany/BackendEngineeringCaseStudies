package com.casestudies.payoutdelivery.platform.payout.application.service;

import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.Money;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;

public final class ScenarioPayout {

    public static final PayoutId PAYOUT_ID = PayoutId.of("PO-9001");
    public static final MerchantId MERCHANT_ID = MerchantId.of("M-1001");
    public static final Money AMOUNT = Money.of("252000", "EGP");

    private ScenarioPayout() {
    }

    public static Payout initialState() {
        return Payout.prepare(PAYOUT_ID, MERCHANT_ID, AMOUNT);
    }
}
