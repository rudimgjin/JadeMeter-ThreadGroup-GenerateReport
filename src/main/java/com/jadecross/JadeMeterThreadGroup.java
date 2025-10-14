/*
 * It has been enhanced with additional features by JADECROSS - IT Medical Center (South Korea)
 * to provide more flexible load testing capabilities.
 * NOTE: This class extends org.apache.jmeter.threads.ThreadGroup to inherit
 * all standard Thread Group functionality (start, stop, etc.) automatically.
 */
package com.jadecross;

import org.apache.jmeter.threads.ThreadGroup; // 기본 ThreadGroup 상속
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JadeMeterThreadGroup: 기본 ThreadGroup의 기능을 모두 상속받고,
 * 부하 공지 및 보고서 생성을 위한 메타데이터 속성을 추가로 저장합니다.
 */
public class JadeMeterThreadGroup extends ThreadGroup {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JadeMeterThreadGroup.class);

    // JMX 속성 정의 (GUI와 연결)
    public static final String SCENARIO_NAME = "JadeMeterThreadGroup.scenarioName";
    public static final String START_TIME = "JadeMeterThreadGroup.startTime";
    public static final String TARGET_RESPONSE_TIME = "JadeMeterThreadGroup.targetResponseTime";
    public static final String TPS_PER_STAGE = "JadeMeterThreadGroup.tpsPerStage";

    // NOTE: ThreadGroup의 기본 속성들 (RAMP_TIME, DURATION, SCHEDULER 등)은
    // 이 클래스가 ThreadGroup을 상속하므로 별도로 재정의할 필요 없이 부모의 것을 사용합니다.

    public JadeMeterThreadGroup() {
        super();
        // 기본값 설정
        setProperty(new StringProperty(SCENARIO_NAME, "Scenario A"));
        setProperty(new StringProperty(START_TIME, "10:00"));
        setProperty(new StringProperty(TARGET_RESPONSE_TIME, "1000ms"));
        setProperty(new StringProperty(TPS_PER_STAGE, ""));

        // Scheduler 기본값 설정 (GUI에서 Specify Thread lifetime 체크박스의 기본 상태)
        setProperty(new BooleanProperty(SCHEDULER, false));
    }

    // --- Getter/Setter 메서드 ---

    public String getScenarioName() { return getPropertyAsString(SCENARIO_NAME); }
    public void setScenarioName(String name) { setProperty(new StringProperty(SCENARIO_NAME, name)); }

    public String getStartTime() { return getPropertyAsString(START_TIME); }
    public void setStartTime(String time) { setProperty(new StringProperty(START_TIME, time)); }

    public String getTargetResponseTime() { return getPropertyAsString(TARGET_RESPONSE_TIME); }
    public void setTargetResponseTime(String rt) { setProperty(new StringProperty(TARGET_RESPONSE_TIME, rt)); }

    public String getTpsPerStage() { return getPropertyAsString(TPS_PER_STAGE); }
    public void setTpsPerStage(String tps) { setProperty(new StringProperty(TPS_PER_STAGE, tps)); }

    // NOTE: 부모 클래스인 ThreadGroup을 상속했기 때문에, JMeter의 부하 실행 코드는
    // 모두 상속받아 사용합니다. 별도의 start(), stop() 등의 구현은 필요 없습니다.
}