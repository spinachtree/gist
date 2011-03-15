#! usr/bin/ruby

# this script assumes the working directory is the directory containing this file.......

puts "compile examples......."
examples=Dir.glob("examples/*.java").join(" ")
system "javac -d class -classpath gist.jar #{examples}"

puts "run Datum..........."
system "java -cp class Datum"

puts "run Calculator..........."
system "java -cp class Calculator"

puts "run Json..........."
system "java -cp class Json"

puts "run Tat..........."
system "java -cp class Tat examples/testTat.txt"

