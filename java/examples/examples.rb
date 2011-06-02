#! usr/bin/ruby

# this script assumes the working directory = gist/java/ 

puts "compile examples......."
examples=Dir.glob("examples/*.java").join(" ")
system "javac -d class -classpath gist.jar #{examples}"

puts "run DateReader..........."
system "java -cp class DateReader"

puts "run DateMap..........."
system "java -cp class DateMap"

puts "run Datum..........."
system "java -cp class Datum"

puts "run Calculator..........."
system "java -cp class Calculator"

puts "run Json..........."
system "java -cp class Json"

