#! usr/bin/ruby

# this script assumes the working directory is the directory containing this file.......

puts "compile gist..........."
files=Dir.glob("src/org/spinachtree/gist/*.java").join(" ")
system "javac -d class -sourcepath src #{files}"

# puts "run Test..........."
# system "java -cp class org.spinachtree.gist.Test"
# 
# puts "run Boot test..........."
# system "java -cp class org.spinachtree.gist.Boot"

system "ruby example.rb"

puts "run PBNF.........."
system "java -cp class org.spinachtree.gist.PBNF"


puts "gist.jar......."
system "jar cf gist.jar -C class org/spinachtree/gist"


puts "compile tests......."
tests=Dir.glob("tests/*.java").join(" ")
system "javac -d class -classpath gist.jar #{tests}"

# puts "run Test1..........."
# system "java -cp class Test1"

system "ruby test.rb"


puts "javadoc......."
system "javadoc -d javadoc -sourcepath src org.spinachtree.gist"

