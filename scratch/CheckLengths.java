package scratch;

import arcadeos.ArchiveManualData;

public class CheckLengths {
    public static void main(String[] args) {
        boolean allGood = true;
        for (int m = 0; m < 8; m++) {
            ArchiveManualData.ManualEntry entry = ArchiveManualData.getManual(m);
            String[] tabs = { entry.theory, entry.formulas, entry.pseudocode, entry.workedExample, entry.cheatSheet };
            String[] tabNames = { "01 THEORY", "02 FORMULAS", "03 PSEUDOCODE", "04 WORKED TRACE", "05 CHEAT SHEET" };

            for (int t = 0; t < 5; t++) {
                String[] lines = tabs[t].split("\n");
                for (int l = 0; l < lines.length; l++) {
                    String line = lines[l];
                    if (line.length() > 74) {
                        System.out.printf("OVER 74! Module %d (%s) Tab %s Line %d: len=%d -> '%s'%n",
                                m + 1, entry.title, tabNames[t], l + 1, line.length(), line);
                        allGood = false;
                    }
                }
            }
        }
        if (allGood) {
            System.out.println("PERFECT! All 8 Modules and all 40 tabs have lines <= 74 characters!");
        }
    }
}
