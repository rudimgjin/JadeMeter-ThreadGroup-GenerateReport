/*
 * It has been enhanced with additional features by JADECROSS - IT Medical Center (South Korea)
 * to provide more flexible load testing capabilities.
 */
package com.jadecross;

import static org.apache.jmeter.util.JMeterUtils.labelFor;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.border.Border;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.gui.JBooleanPropertyEditor;
import org.apache.jmeter.gui.JTextComponentBinding;
import org.apache.jmeter.gui.TestElementMetadata;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.AbstractThreadGroupSchema;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.ThreadGroupSchema;
import org.apache.jmeter.threads.gui.AbstractThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.JEditableCheckBox;

import net.miginfocom.swing.MigLayout;

// [수정] 메타데이터를 StaticLabel과 동일하게 설정하여 리소스 로딩 문제를 우회
//@TestElementMetadata(labelResource = "JadeMeter - Thread Group - GenReport")
public class JadeMeterThreadGroupGui extends AbstractThreadGroupGui
        implements ItemListener, FocusListener
{
    private static final long serialVersionUID = 240L;

    private LoopControlPanel loopPanel;

    // JadeMeter 추가 필드 및 컴포넌트 선언
    private final JTextField scenarioNameField = new JTextField();
    private final JTextField targetTpsField = new JTextField();
    private final JTextField targetResponseTimeField = new JTextField("1000ms");
    private final JTextField startTimeField = new JTextField(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

    // JMeter 기본 필드
    private final JTextField threadInput = new JTextField();
    private final JTextField rampInput = new JTextField("1");
    private final JTextField duration = new JTextField("0");
    private final JTextField delay = new JTextField();
    private boolean showDelayedStart;

    // 기타 JMeter 기본 필드
    private JBooleanPropertyEditor delayedStart;
    private final JCheckBox schedulerBox = new JCheckBox(JMeterUtils.getResString("scheduler"));
    private final JBooleanPropertyEditor schedulerBinding = new JBooleanPropertyEditor(
            ThreadGroupSchema.INSTANCE.getUseScheduler(), JMeterUtils.getResString("scheduler"));

    private final JLabel delayLabel = new JLabel(JMeterUtils.getResString("delay"));
    private final JLabel durationLabel = new JLabel(JMeterUtils.getResString("duration"));
    private final JBooleanPropertyEditor sameUserBox = new JBooleanPropertyEditor(
            AbstractThreadGroupSchema.INSTANCE.getSameUserOnNextIteration(), JMeterUtils.getResString("threadgroup_same_user"));

    // 리포트 컴포넌트
    private final JTextArea reportArea = new JTextArea(10, 50);
    private final JButton copyButton = new JButton("부하 공지 멘트 클립보드 복사");

    // ------------------------------------------------------------------
    // 생성자 1 & 2
    // ------------------------------------------------------------------
    public JadeMeterThreadGroupGui() {
        this(true);
    }

    public JadeMeterThreadGroupGui(boolean showDelayedStart) {
        super();
        this.showDelayedStart = showDelayedStart;
        this.loopPanel = createControllerPanel();

        init();
        initGui();

        // 리스너 등록
        Stream.of(threadInput, rampInput, duration, delay, scenarioNameField, targetTpsField, targetResponseTimeField, startTimeField)
                .forEach(field -> field.addFocusListener(this));

        schedulerBox.addItemListener(this);
        copyButton.addActionListener(e -> updateReportAndCopyToClipboard());

        updateReportTextOnly();

        if (showDelayedStart) {
            delayedStart = new JBooleanPropertyEditor(
                    ThreadGroupSchema.INSTANCE.getDelayedStart(),
                    JMeterUtils.getResString("delayed_start"));
            bindingGroup.add(delayedStart);
        }
        bindingGroup.addAll(
                Arrays.asList(
                        new JTextComponentBinding(threadInput, AbstractThreadGroupSchema.INSTANCE.getNumThreads()),
                        new JTextComponentBinding(rampInput, ThreadGroupSchema.INSTANCE.getRampTime()),
                        new JTextComponentBinding(duration, ThreadGroupSchema.INSTANCE.getDuration()),
                        new JTextComponentBinding(delay, ThreadGroupSchema.INSTANCE.getDelay()),
                        sameUserBox,
                        schedulerBinding
                )
        );
    }

    private LoopControlPanel createControllerPanel() {
        LoopControlPanel panel = new LoopControlPanel(false);
        LoopController looper = (LoopController) panel.createTestElement();
        looper.setLoops(1);
        panel.configure(looper);
        return panel;
    }


    @Override
    public TestElement makeTestElement() {
        return new JadeMeterThreadGroup();
    }

    @Override
    public void assignDefaultValues(TestElement element) {
        super.assignDefaultValues(element);

        element.setProperty(AbstractThreadGroupSchema.INSTANCE.getNumThreads().getName(), "1");
        element.setProperty(ThreadGroupSchema.INSTANCE.getRampTime().getName(), "1");
        element.setProperty(new BooleanProperty(AbstractThreadGroupSchema.INSTANCE.getSameUserOnNextIteration().getName(), true));
        element.setProperty(new BooleanProperty(ThreadGroupSchema.INSTANCE.getUseScheduler().getName(), false));

        ((AbstractThreadGroup) element).setSamplerController((LoopController) loopPanel.createTestElement());
    }

    @Override
    public void modifyTestElement(TestElement tg) {
        super.modifyTestElement(tg);
        if (tg instanceof AbstractThreadGroup) {
            ((AbstractThreadGroup) tg).setSamplerController((LoopController) loopPanel.createTestElement());
        }

        tg.setProperty(new BooleanProperty(ThreadGroup.SCHEDULER, schedulerBox.isSelected()));

        if (tg instanceof JadeMeterThreadGroup) {
            JadeMeterThreadGroup utg = (JadeMeterThreadGroup) tg;
            utg.setScenarioName(scenarioNameField.getText());
            utg.setStartTime(startTimeField.getText());
            utg.setTpsPerStage(targetTpsField.getText());
            utg.setTargetResponseTime(targetResponseTimeField.getText());
        }
    }

    @Override
    public void configure(TestElement tg) {
        super.configure(tg);
        loopPanel.configure((TestElement) tg.getProperty(AbstractThreadGroup.MAIN_CONTROLLER).getObjectValue());

        if (tg instanceof JadeMeterThreadGroup) {
            JadeMeterThreadGroup utg = (JadeMeterThreadGroup) tg;
            scenarioNameField.setText(utg.getScenarioName());
            startTimeField.setText(utg.getStartTime());
            targetTpsField.setText(utg.getTpsPerStage());
            targetResponseTimeField.setText(utg.getTargetResponseTime());
        }

        schedulerBox.setSelected(tg.getPropertyAsBoolean(ThreadGroup.SCHEDULER));

        toggleSchedulerFields();
        updateReportTextOnly();
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource() == schedulerBox) {
            toggleSchedulerFields();
            updateReportAndCopyToClipboard();
        }
    }

    private void toggleSchedulerFields() {
        boolean enable = schedulerBox.isSelected();
        duration.setEnabled(enable);
        durationLabel.setEnabled(enable);
        delay.setEnabled(enable);
        delayLabel.setEnabled(enable);
    }

    @Override
    public String getLabelResource() {

        return null;
    }

    @Override
    public String getStaticLabel() {
        return "JadeMeter-ThreadGroup-GenReport";
    }

    @Override
    public void clearGui(){
        super.clearGui();
        initGui();
        updateReportTextOnly();
    }

    private void initGui(){
        loopPanel.clearGui();
        scenarioNameField.setText("Scenario A");
        targetTpsField.setText("");
        targetResponseTimeField.setText("1000ms");
        startTimeField.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        rampInput.setText("1");
        duration.setText("0");
        schedulerBox.setSelected(false);
        toggleSchedulerFields();
    }

    private void init() {
        JPanel mainPanel = new VerticalPanel();


        Border titleBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), getStaticLabel());
        setBorder(titleBorder);


        // [1] Thread Properties (가장 위로)
        // ------------------------------------------------------------------
        JPanel threadPropsPanel = createThreadPropertiesPanel();
        mainPanel.add(threadPropsPanel);

        // ------------------------------------------------------------------
        // [2] 부하 공지 섹션: 수평으로 2개 패널 나란히 배치 (아래로)
        // ------------------------------------------------------------------
        JPanel horizontalReportSection = new JPanel(new MigLayout("fillx, wrap 2", "[fill, grow][fill, grow]"));

        JPanel reportInputPanel = createReportInputPanel();
        horizontalReportSection.add(reportInputPanel, "grow, hmin 200");

        JPanel reportOutputPanel = createReportPanel();
        horizontalReportSection.add(reportOutputPanel, "grow, hmin 200, wrap");

        mainPanel.add(horizontalReportSection);

        add(mainPanel, BorderLayout.CENTER);
    }

    // --- 헬퍼 메서드: Thread Properties Panel 생성 ---
    private JPanel createThreadPropertiesPanel() {
        JPanel threadPropsPanel = new JPanel(new MigLayout("fillx, wrap 2", "[][fill,grow]"));
        threadPropsPanel.setBorder(BorderFactory.createTitledBorder(JMeterUtils.getResString("thread_properties")));

        // labelFor 대신 new JLabel 사용 (하드코딩된 문자열 사용)
        threadPropsPanel.add(new JLabel("Number of Threads (users)"));
        threadPropsPanel.add(threadInput);

        threadPropsPanel.add(new JLabel("Ramp-up period (seconds)"));
        threadPropsPanel.add(rampInput);

        // LOOP COUNT
        threadPropsPanel.add(loopPanel.getLoopsLabel(), "split 2");
        threadPropsPanel.add(loopPanel.getInfinite(), "gapleft push");
        threadPropsPanel.add(loopPanel.getLoops());

        threadPropsPanel.add(sameUserBox, "span 2");
        if (showDelayedStart) {
            if (delayedStart == null) {
                delayedStart = new JBooleanPropertyEditor(ThreadGroupSchema.INSTANCE.getDelayedStart(), JMeterUtils.getResString("delayed_start"));
            }
            threadPropsPanel.add(delayedStart, "span 2");
        }

        // Scheduler 체크박스 위치
        threadPropsPanel.add(schedulerBox, "span 2");

        threadPropsPanel.add(durationLabel);
        threadPropsPanel.add(duration);
        threadPropsPanel.add(delayLabel);
        threadPropsPanel.add(delay);

        return threadPropsPanel;
    }

    // --- 헬퍼 메서드: Report Input Panel 생성 ---
    private JPanel createReportInputPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx, wrap 2", "[100!][fill, grow]"));
        panel.setBorder(BorderFactory.createTitledBorder("부하 공지용 시나리오 정보"));

        panel.add(new JLabel("시나리오 이름:"));
        panel.add(scenarioNameField);

        panel.add(new JLabel("목표 TPS:"));
        panel.add(targetTpsField);

        panel.add(new JLabel("목표 응답시간:"));
        panel.add(targetResponseTimeField);

        panel.add(new JLabel("시작 시간 (HH:mm):"));
        panel.add(startTimeField);

        return panel;
    }

    // --- 리포트 패널 생성 메서드 ---

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("부하 공지 멘트 (자동생성 및 클립보드 복사)"));

        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);

        // 복사 버튼 패널
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(copyButton, BorderLayout.WEST);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        return panel;
    }

    // --- FocusListener 구현 ---

    @Override public void focusGained(FocusEvent e) {}
    @Override
    public void focusLost(FocusEvent e) {
        updateReportAndCopyToClipboard();
    }

    // --- 클립보드 및 멘트 생성 로직 ---

    private void updateReportTextOnly() {
        String reportText = generateReportText();
        reportArea.setText(reportText);
    }

    private void updateReportAndCopyToClipboard() {
        String reportText = generateReportText();
        reportArea.setText(reportText);

        try {
            StringSelection stringSelection = new StringSelection(reportText);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        } catch (IllegalStateException e) {
            reportArea.setText(reportText + "\n\n[경고] 클립보드에 복사할 수 없습니다. 버튼을 다시 눌러주세요.");
        }
    }

    private String generateReportText() {
        String scenarioName = scenarioNameField.getText().trim().isEmpty() ? "Undefined" : scenarioNameField.getText().trim();

        String targetTps = targetTpsField.getText().trim().isEmpty() ? "[입력되지 않음]" : targetTpsField.getText().trim();
        String targetRT = targetResponseTimeField.getText().trim().isEmpty() ? "1000ms" : targetResponseTimeField.getText().trim();
        String startTimeStr = startTimeField.getText().trim();

        int rampUp = parseNumericField(rampInput.getText(), 0);

        long durationSec = schedulerBox.isSelected() ? parseNumericField(duration.getText(), 0) : 0;

        long totalSeconds = rampUp + durationSec;

        String loadTimeStr = "시작 시간을 'HH:mm' 형식으로 입력하세요.";
        try {
            LocalDate today = LocalDate.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime startTime = LocalTime.parse(startTimeStr, timeFormatter);
            LocalTime endTime = startTime.plusSeconds(totalSeconds);

            loadTimeStr = String.format("%s %s - %s", today, startTime.format(timeFormatter), endTime.format(timeFormatter));

        } catch (DateTimeParseException e) {
            // 시간 형식 오류 처리
        }

        return String.format(
                "[%s 테스트 부하모델상세]\n" +
                        "부하 진행 시간 : %s\n\n" +
                        "목표 TPS : %s\n" +
                        "Ramp-Up 시간 : %d초\n" +
                        "유지시간 : %d초\n" +
                        "목표 응답시간 : %s",
                scenarioName,
                loadTimeStr,
                targetTps,
                rampUp,
                durationSec,
                targetRT
        );
    }

    private int parseNumericField(String text, int defaultValue) {
        try {
            return Integer.parseInt(text.trim().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
