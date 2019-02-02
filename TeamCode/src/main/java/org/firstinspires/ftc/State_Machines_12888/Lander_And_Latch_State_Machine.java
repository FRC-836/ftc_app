package org.firstinspires.ftc.State_Machines_12888;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

enum State_Enum {
    RAISING,
    LOWERING,
    UNLOCKING,
    STANDBY
}

public class Lander_And_Latch_State_Machine implements Runnable {

    private CRServo latchLockServo;
    private DcMotor landingMotor;
    private State_Enum currentState;
    private ElapsedTime servoTimer = new ElapsedTime();
    private ElapsedTime lockingTimer = new ElapsedTime();
    private AtomicBoolean opModeIsActive = new AtomicBoolean(true);
    private final double DEPLOY_POWER = 0.4;
    private final double UNLOCKING_POWER = 0.5;
    private final double LOCKED_POWER = 0.0;
    private final double LOCKING_POWER = -0.5;
    private final double UNLCOKED_POWER = 0.25;

    private Semaphore stateAndTimerLock = new Semaphore(1);
    private Semaphore timerLock = new Semaphore(1);

    public Lander_And_Latch_State_Machine(HardwareMap hardwareMap) {
        latchLockServo = hardwareMap.get(CRServo.class, "ll");
        landingMotor = hardwareMap.get(DcMotor.class, "lm");

        latchLockServo.setDirection(CRServo.Direction.FORWARD);
        landingMotor.setDirection(DcMotor.Direction.REVERSE);

        landingMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        setState(State_Enum.STANDBY);
    }

    @Override
    public void run() {
        while (opModeIsActive.get()) {
            switch (getState()) {
                case RAISING:
                    landingMotor.setPower(DEPLOY_POWER);
                    latchLockServo.setPower(UNLCOKED_POWER);
                    break;
                case LOWERING:
                    landingMotor.setPower(-DEPLOY_POWER);
                    lockServo();
                    break;
                case UNLOCKING:
                    landingMotor.setPower(0.0);
                    latchLockServo.setPower(UNLOCKING_POWER);
                    while (!timerLock.tryAcquire()) {
                        sleep(0);
                    }
                    if (servoTimer.milliseconds() > 500.0) {
                        setState(State_Enum.RAISING);
                    }
                    timerLock.release();
                    break;
                case STANDBY:
                    landingMotor.setPower(0.0);
                    lockServo();
                    break;
            }
            sleep(15);
        }
    }

    private synchronized State_Enum getState() {
        while (!stateAndTimerLock.tryAcquire()) {
            sleep(0);
        }
        State_Enum state = currentState;
        stateAndTimerLock.release();

        return state;
    }

    private synchronized void setState(State_Enum state) {
        while (!stateAndTimerLock.tryAcquire()) {
            sleep(0);
        }
        lockingTimer.reset();
        currentState = state;
        stateAndTimerLock.release();
    }

    public void raise() {
        if (getState() == State_Enum.STANDBY || getState() == State_Enum.LOWERING) {
            setState(State_Enum.UNLOCKING);
            while (!timerLock.tryAcquire()) {
                sleep(0);
            }
            servoTimer.reset();
            timerLock.release();
        }
    }

    public void lower() {
        setState(State_Enum.LOWERING);
    }

    public void stopLatch() {
        setState(State_Enum.STANDBY);
    }

    public void stopThread() {
        opModeIsActive.set(false);
    }

    private synchronized void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void lockServo() {
        while (!stateAndTimerLock.tryAcquire()) {
            sleep(0);
        }
        if (lockingTimer.milliseconds() < 500.0)
            latchLockServo.setPower(LOCKING_POWER);
        else
            latchLockServo.setPower(LOCKED_POWER);
        stateAndTimerLock.release();
    }
}
/*
abstract class State_Class {
}

class Raising extends State_Class {
}

class Lowering extends State_Class {
}

class Unlocking extends State_Class {
}

class Standby extends State_Class {
}

*/