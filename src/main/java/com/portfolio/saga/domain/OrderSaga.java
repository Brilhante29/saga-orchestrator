package com.portfolio.saga.domain;

import com.portfolio.saga.steps.ProcessPaymentStep;
import com.portfolio.saga.steps.ReserveInventoryStep;
import com.portfolio.saga.steps.ShipOrderStep;

import java.util.List;

public class OrderSaga extends Saga {

    public OrderSaga(SagaLog sagaLog, String input,
                     boolean failInventory, boolean failPayment, boolean failShip) {
        super(List.of(
                new ReserveInventoryStep(failInventory),
                new ProcessPaymentStep(failPayment),
                new ShipOrderStep(failShip)
        ), sagaLog, input);
    }

    public OrderSaga(SagaLog sagaLog, String input, int failAtStep) {
        super(List.of(
                new ReserveInventoryStep(failAtStep == 1),
                new ProcessPaymentStep(failAtStep == 2),
                new ShipOrderStep(failAtStep == 3)
        ), sagaLog, input);
    }

    public OrderSaga(SagaLog sagaLog, String input) {
        this(sagaLog, input, false, false, false);
    }
}
