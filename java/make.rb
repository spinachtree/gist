#! usr/bin/ruby

# this script assumes the working directory is: gist/java/

puts "compile gist..........."
files=Dir.glob("src/org/spinachtree/gist/*.java").join(" ")
system "javac -d class -sourcepath src #{files}"

# puts "run Test........... first development tests..."
# system "java -cp class org.spinachtree.gist.Test"
# 
# puts "run Boot test........... development tests..."
# system "java -cp class org.spinachtree.gist.Boot"

# -- make a new gist.jar ----------------------------------------------------

puts "gist.jar......."
system "jar cf gist.jar -C class org/spinachtree/gist"


# -- run examples -----------------------------------------------------------

system "ruby examples/examples.rb"

# -- testing and performance.... ---------------------------------------------

# puts "run PBNF.........."
# system "java -cp class org.spinachtree.gist.PBNF"


# -- compile and run test cases ---------------------------------------------

system "ruby tests/tests.rb"


# -- javadoc ----------------------------------------------------------------

puts "javadoc......."
system "javadoc -d javadoc -sourcepath src org.spinachtree.gist"

