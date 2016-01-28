#!/usr/bin/env ruby

chunk = 0
File.read('table.csv').lines.each_slice(1000) do |lines|
    File.open("src/com/hitechbunny/table/Chunk#{chunk}.java", "w") do |f|
        f << "package com.hitechbunny.table;\n"
        f << "import com.hitechbunny.Table;\n"
        f << "public class Chunk#{chunk} {\n"
        f << "  public static void addAll() {\n"
        lines.each do |line|
            parts = line.strip.split(',')
            f << "    Table.add(\"#{parts[0]}\",#{parts[1] == '' ? parts[2] : parts[1]});"
        end
        f << "   }\n"
        f << "}\n"
    end
    chunk += 1
end

File.open("src/com/hitechbunny/Table.java", "w") do |f|
    f << "package com.hitechbunny;\n"

    f << "import java.util.HashMap;\n"
    f << "import java.util.Map;\n"

    f << "public class Table {\n"
    f << "    public static final Map<String, Rollout> table = new HashMap<>(1000000);\n"

    f << "    public static void add(String key, double rlScore) {\n"
    f << "        Rollout r = new Rollout();\n"
    f << "        r.rlScore = rlScore;\n"
    f << "        table.put(key, r);\n"
    f << "    }\n"
    f << "    static {\n";
    0.upto(chunk-1) do |i|
        f << "        com.hitechbunny.table.Chunk#{i}.addAll();\n";
    end
    f << "    }\n"
    f << "}\n"
end