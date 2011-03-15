#! usr/bin/ruby

# this script assumes the working directory is the directory containing this file.......

puts "compile examples......."
examples=Dir.glob("examples/*.java").join(" ")
system "javac -d class -classpath gist.jar #{examples}"

puts "transform doc.txt files into doc.txt.html ..........."

files=Dir.glob("doc/*.txt").join(" ")
system "java -cp class Tat #{files}"
