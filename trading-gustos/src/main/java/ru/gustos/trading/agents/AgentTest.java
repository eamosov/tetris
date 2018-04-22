package ru.gustos.trading.agents;

import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class AgentTest {
    public static void main(String[] args) throws Exception {
        Sheet sheet = TestUtils.makeSheet();
        AgentManager m = new AgentManager(sheet);
        m.init(20,sheet.getBarIndex(ZonedDateTime.of(2018,1,1,0,0,0,0, ZoneId.systemDefault())));
        m.runTillEnd();
    }
}
