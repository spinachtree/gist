#! usr/bin/ruby

# this script assumes the working directory is the directory containing this file.......


puts "compile tests......."
tests=Dir.glob("tests/*.java").join(" ")
system "javac -d class -classpath gist.jar #{tests}"

# puts "run Test..........."
# system "java -cp class Test1"

puts "run Tests..........."
system "java -cp class Tests tests/basics.tat tests/priors.tat"

