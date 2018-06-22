# set output 'jmh-results.png'
# set output 'jmh-results.svg'
# set output 'jmh-results.pdf'
# set terminal pdf
# set terminal pdfcairo
# set terminal pdfcairo size 300,300
# set terminal pdfcairo size 6,2
set terminal pdfcairo size 7,2

# show size
# set size 1,0.5

set datafile separator ','

# --- start common commands ---
set border 1+2
set xlabel font ", 10"
set ylabel font ", 10"
set xtics font ", 8"
set ytics font ", 8"
set ytics nomirror
set xtics nomirror
#set rmargin at screen 0.975
#set lmargin at screen 0.05
# 
# show margin
# set bmargin 500
# set bmargin at screen 0.3
set bmargin at screen 0.2
# set bmargin 0
# show margin
# --- end common commands ---

# set key outside above horizontal maxrows 2
# set key left
set key left horizontal
set key font ", 10"
#Left reverse horizontal maxcols 1 at screen 1.01, 0.96 opaque
set boxwidth 0.8 relative

set xtics auto
set style data histogram 
set style histogram cluster gap 1 
set style histogram errorbars gap 2 lw 1

# set xtic rotate by 45 right
set xtic rotate by 15 right
# set xtic rotate by 10 right  # still legile but becomes ugly
set xtics 0.25 nomirror scale 0 

set grid front
unset grid
unset mytics

# set ylabel "Normalized Execution Time" offset 2,0 # TODO?
# set ylabel "Operations per second" offset 2,0
set ylabel "Speedup" offset 2,0
# set yrange [0.5:*]

set style fill solid noborder

# set logscale y
plot \
	'jmh-results-p.csv' using 2:3:xtic(1) title "baseline", \
	1 title "", \
	'jmh-results-p.csv' using 4:5:xtic(1) title "for", \
	'jmh-results-p.csv' using 6:7:xtic(1) title "lazy for", \
	'jmh-results-p.csv' using 8:9:xtic(1) title "lazy for, fused", \
	'jmh-results-p.csv' using 10:11:xtic(1) title "lazy for, mixed", \
	'jmh-results-p.csv' using 12:13:xtic(1) title "lazy for, unboxed", \
	'jmh-results-p.csv' using 14:15:xtic(1) title "monadic for", \
	'jmh-results-p.csv' using 16:17:xtic(1) title "lifted for"


