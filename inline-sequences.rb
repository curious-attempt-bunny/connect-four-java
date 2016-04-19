java = File.read("src/com/hitechbunny/Opening.java")
seqs = File.read("sequences.txt")
java.sub!(/(?mi)static {.*?}/, "static {\n#{seqs}\n}")
File.write("src/com/hitechbunny/Opening.java", java)
