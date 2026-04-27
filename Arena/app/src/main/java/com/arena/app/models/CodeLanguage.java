package com.arena.app.models;

public enum CodeLanguage {
    JAVA(62, "Java", String.join("\n",
            "import java.io.*;",
            "import java.util.*;",
            "",
            "public class Main {",
            "    public static void main(String[] args) throws Exception {",
            "        Scanner scanner = new Scanner(System.in);",
            "        // Write your solution for %s here.",
            "        System.out.println(\"Ready to solve %s\");",
            "    }",
            "}")),
    CPP(54, "C++17", String.join("\n",
            "#include <bits/stdc++.h>",
            "using namespace std;",
            "",
            "int main() {",
            "    ios::sync_with_stdio(false);",
            "    cin.tie(nullptr);",
            "",
            "    // Write your solution for %s here.",
            "    cout << \"Ready to solve %s\" << '\\n';",
            "    return 0;",
            "}")),
    PYTHON(71, "Python 3", String.join("\n",
            "# Write your solution for %s here.",
            "def solve():",
            "    print(\"Ready to solve %s\")",
            "",
            "",
            "if __name__ == \"__main__\":",
            "    solve()")),
    JAVASCRIPT(63, "JavaScript", String.join("\n",
            "'use strict';",
            "",
            "// Write your solution for %s here.",
            "function solve() {",
            "  console.log('Ready to solve %s');",
            "}",
            "",
            "solve();"));

    private final int languageId;
    private final String displayName;
    private final String template;

    CodeLanguage(int languageId, String displayName, String template) {
        this.languageId = languageId;
        this.displayName = displayName;
        this.template = template;
    }

    public int getLanguageId() {
        return languageId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String buildTemplate(String problemTitle) {
        String safeTitle = problemTitle == null || problemTitle.trim().isEmpty()
                ? "this problem"
                : problemTitle.trim();
        return String.format(template, safeTitle, safeTitle);
    }

    public static String[] getDisplayNames() {
        CodeLanguage[] values = values();
        String[] names = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            names[index] = values[index].displayName;
        }
        return names;
    }

    public static CodeLanguage fromDisplayName(String displayName) {
        for (CodeLanguage language : values()) {
            if (language.displayName.equalsIgnoreCase(displayName)) {
                return language;
            }
        }
        return JAVA;
    }
}
