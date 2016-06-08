(ns computer-systems.ch2
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.string :as clj-str]))


(def ex2-2 [[1 0 1 1] [1 1 0 1 1] [1 1 1 0 1 1]])


(defn twos-complement-pos [bit-vector]
  (let [rev-vec (reverse bit-vector)]
  (loop [power 0
         rv    0]
    (if (= power (count bit-vector))
     rv 
     (recur
      ;power
      (+ power 1)

      ;rv
      (+ rv (*
             (nth rev-vec power) ;is the bit positive
             (Math/pow 2  power))))))))

(defn twos-complement-neg [bit-vector]
  (let [neg-val (Math/pow 2 (- (count bit-vector) 1))]
    (- 
      (twos-complement-pos
        ;invert the vector
        ;(map #(if (= 1 %1) 0 1) (rest bit-vector)))
        (rest bit-vector))
      neg-val)))

(defn twos-complement [bit-vector]
  (if (= (first bit-vector) 1)
    (int (twos-complement-neg bit-vector))
    (int (twos-complement-pos bit-vector))))


;practice problem 2.24
;Suppose we truncate a 4-bit value (represented by hex digits 0 through F) to a 3- bit value (represented as hex digits 0 through 7). Fill in the table below showing the effect of this truncation for some cases, in terms of the unsigned and two’s- complement interpretations of those bit patterns.

(def unsigned-nums
  [[0 0 0 0] ;0
  [0 0 1 0] ;2
  [1 0 0 1] ;9
  [1 0 1 1] ;11
  [1 1 1 1]]) ;15

(def twos-complement-nums
  [[0 0 0 0] ;0
   [0 0 1 0] ;2
   [1 0 0 1] ;-7
   [1 0 1 1] ;-5
   [1 1 1 1]];-1
  )
  

(defn truncate-unsigned [bit-vec]
  (twos-complement-pos (rest bit-vec)))

(defn truncate-twos-complement [bit-vec]
  (twos-complement (rest bit-vec)))

(defn run-2-24 []
  (println (str "Unsigned ints: "
    (pr-str (map
      #(truncate-unsigned %1)
      unsigned-nums))))
  (println (str "Twos complement: "
    (pr-str (map
      #(truncate-twos-complement %1)
      twos-complement-nums)))))


;=> Unsigned ints: (0.0 2.0 1.0 3.0 7.0) ;satisfies the condition mod 2^k
;=> Twos complement:  (0 2 1 3 -1)
; Note, the twos complement is not what the formula asked for. It asked for the transition from twos complement to unsigned to truncated. This would result in the same values as the Unsigned ints: My way is certianly more interesting!




;Practice problem 2.25.
;The reason this results in an error is that length is unsigned. Casting a -1 to unsigned yields unsigned max, therefore it will iterate unsigned max times. The following code should iterate many many many times

(def buggy-c-code
  "#include <stdio.h>

   float sum_elements(float a[], unsigned length){
      int i;
      int sum;
      for(i=0; i<=length-1; i++){
        printf(\"%d\", i);
      }
      return 0.0;
    }
   int main(){
     float elements[0];
     sum_elements(elements ,0);
   }
  ")

;to fix use i<length

(defmacro with-timeout [millis & body]
  `(let [future# (future ~@body)]
    (try
      (.get future# ~millis java.util.concurrent.TimeUnit/MILLISECONDS)
      (catch java.util.concurrent.TimeoutException x# 
        (do
          (future-cancel future#)
          "function timed out")))))

(defn compile-and-run-buggy-c-code []
  (spit "buggy_c.cpp" buggy-c-code)
  (sh "gcc" "-o" "buggy_c" "buggy_c.cpp")
  (with-timeout 1000 (sh "./buggy_c")))

;And I was right!

;2.28 Hex to decimal to negative unsigned complement (way to do subtraction under unsigned integers)

(def hex-to-bin-encoding
                 {"0" [0 0 0 0]
                  "1" [0 0 0 1]
                  "2" [0 0 1 0]
                  "3" [0 0 1 1]
                  "4" [0 1 0 0]
                  "5" [0 1 0 1]
                  "6" [0 1 1 0]
                  "7" [0 1 1 1]
                  "8" [1 0 0 0]
                  "9" [1 0 0 1]
                  "A" [1 0 1 0]
                  "B" [1 0 1 1]
                  "C" [1 1 0 0]
                  "D" [1 1 0 1]
                  "E" [1 1 1 0]
                  "F" [1 1 1 1]})

(defn hex-to-binary [hex-str]
  (let [encoding {"0" [0 0 0 0]
                  "1" [0 0 0 1]
                  "2" [0 0 1 0]
                  "3" [0 0 1 1]
                  "4" [0 1 0 0]
                  "5" [0 1 0 1]
                  "6" [0 1 1 0]
                  "7" [0 1 1 1]
                  "8" [1 0 0 0]
                  "9" [1 0 0 1]
                  "A" [1 0 1 0]
                  "B" [1 0 1 1]
                  "C" [1 1 0 0]
                  "D" [1 1 0 1]
                  "E" [1 1 1 0]
                  "F" [1 1 1 1]}]
     (flatten (map #(get encoding (str %1)) hex-str))))

(defn decimal-to-binary [dec word-size]
  (loop [decimal-part dec
         binary-part  []]
      (if (= word-size (count binary-part))
        (reverse binary-part)
        (recur (Math/floor (/ decimal-part 2)) (conj binary-part (int (mod decimal-part 2)))))))

(defn binary-to-decimal [bin]
  (let [col (reverse bin)]
    (loop [power 0
         sum   0
         ]
      (if (= power (count col))
        (int sum)
        (recur (+ 1 power) (+ sum (* (nth col power) (Math/pow 2 power))))))))

(defn binary-to-hex [bin]
  (if (= 0 (mod (count bin) 4))
    (let [bin-to-hex (clojure.set/map-invert hex-to-bin-encoding)
          bin (reverse bin)]
      (loop [remaining-bin bin
             result ""]
        (if (not (= 0 (count remaining-bin)))
          (recur (drop 4 remaining-bin) (str result (get  bin-to-hex (reverse (take 4 remaining-bin)))))
          (apply str (reverse result)))))
   (throw (Exception. "Bin must be a multiple of 4"))))

(def input-for-2-28 ["0" "5" "8" "D" "F"])

(defn unsigned-sub [dec word-size]
  (if (= 0 dec)
    0
    (int (- (Math/pow 2 word-size) dec))))

(defn solve-2-28 []
  (map
   #(-> %1
        hex-to-binary
        binary-to-decimal
        ((fn [item] do (println (str %1 " as dec: " item)) item))
        (unsigned-sub 4)
        ((fn [item] do (println (str "unsigned-complement of " %1 " as dec: " item)) item))
        (decimal-to-binary 4)
        binary-to-hex
        ((fn [item] do (println (str "unsigned-complement of " %1 " as hex: " item)) item)))
   input-for-2-28))



;Practice 2.29 Fill in the following table in the style of Figure 2.24. Give the integer values of the 5-bit arguments, the values of both their integer and two’s-complement sums, the bit-level representation of the two’s-complement sum, and the case from the derivation of Equation 2.14.

(defn pad-twos-complement [x word-size]
  (if (< (count x) word-size)
    (let [padding-type (first x)
          padding (repeat (- word-size (count x)) padding-type)]
      (concat padding x))
    x))

(defn non-overflow-sum [x y]
  (let [x (reverse (pad-twos-complement x (count y)))
        y (reverse (pad-twos-complement y (count x)))]
    (loop [index 0
           carry  0
           result []]
      (if (and (>= index (count x)) (= carry 0))
        (reverse result)
        (recur
          (+ index 1)
          (if (> (+ carry (nth x index 0) (nth y index 0)) 1) 1 0) ;These could be done bitwise with &
          (conj result (mod (+ carry (nth x index 0) (nth y index 0)) 2)))))))

;This is broken.

(defn overflow-sum [x y]
  (let [x (reverse (pad-twos-complement x (count y)))
        y (reverse (pad-twos-complement y (count x)))]
    (loop [index 0
           carry 0
           result []]
      (if (>= index (count x))
        (reverse result)
        (recur
          (+ index 1)
          (if (> (+ carry (nth x index 0) (nth y index 0)) 1) 1 0) ;These could be done bitwise with &
          (conj result (mod (+ carry (nth x index 0) (nth y index 0)) 2)))))))


(def data-2-29 [
  [[1 0 1 0 0][1 0 0 0 1]]
  [[1 1 0 0 0][1 1 0 0 0]]
  [[1 0 1 1 1][0 1 0 0 0]]
  [[0 0 0 1 0][0 0 1 0 1]]
  [[0 1 1 0 0][0 0 1 0 0]]])

(defn solve-2-29 []
  (map
    #(do
      ((fn [input] (println (str (first input) "+" (second input) " non-overflow =: "
          (non-overflow-sum (first input) (second input))))) %1)
       ((fn [input] (println (str (first input) "+" (second input) " overflow =: "
          (overflow-sum (first input) (second input))))) %1 )
      (println ""))
    data-2-29))

 
;Practice 2.30 Write a function with the following prototype:
;/* Determine whether arguments can be added without overflow */
;int tadd_ok(int x, int y);

(defn tadd_ok [x y]
  (let [sum (overflow-sum x y)]
    (if (and (= (first x) (first y)) (not (= (first sum) (first x))))
      0
      1)))

;(tadd_ok [0 1 1 0] [0 0 0 1]) => 1
;(tadd_ok [0 1 1 1] [0 0 0 1]) => 0
;(tadd_ok [1 0 0 1] [1 0 1 0]) => 0
;(tadd_ok [1 1 1 1] [1 1 1 0]) => 1
;(tadd_ok [1 1 1 1] [0 1 1 0]) => 1

;2.31
;Good, I am on the right track. Addition is reversable. Even if x + y overflows, sum - x will just reverse the overflow. You always end up with x

;2.32
(defn tsub-ok [x y]
  (tadd_ok x y))

(defn log2 [n]
  (/ (Math/log n) (Math/log 2)))

(defn get-min-word-size [num]
  (+ 2 (Math/floor (log2 num))))

(defn test-tsub-ok []
  (doseq [x (map (fn [item] [(decimal-to-binary (* -1 item) 4) item]) (range -8 8))]
    (doseq [y (map (fn [item] [(decimal-to-binary (* -1 item) 4) item]) (range -8 8))]
        (println "(" x "," y "): " (tsub-ok (first x) (first y))
                 (<= (Math/abs (- (second x) (second y))) 8)))))


;Practice Problem 2.33
;We can represent a bit pattern of length w = 4 with a single hex digit. For a two’s- complement interpretation of these digits, fill in the following table to determine the additive inverses of the digits shown:

(def data-2-33 
  [{:hex "0"}
   {:hex "5"}
   {:hex "8"}
   {:hex "D"}
   {:hex "F"}])

(defn twos-complement-additive-inverse [item size]
  (cond   
    (or (> item (Math/pow 2 (- size 1))) (< item (* -1 (Math/pow 2 (- size 1)))))
    (throw (Exception. (str
                        item
                        " exceeds size representable by 2s complement with word size " 
                        size)))

    (== item (* -1 (Math/pow 2 (- size 1))))
    (* -1 (Math/pow 2 (- size 1)))


    :else
    (* -1 item)))



(defn solve-2-33 []
  (map
   #(let
        [dec        (twos-complement (hex-to-binary (:hex %1)))
         neg-x-dec  (twos-complement-additive-inverse dec 4)]
       (assoc
       %1
       :dec    dec 
       :-x-dec neg-x-dec
       :-x-hex (binary-to-hex (decimal-to-binary neg-x-dec 4))))
       data-2-33))

   
;unsigned and signed additive inverses have the same bit representation. This now seems obvious. The same bit addition should always produce [0 0 0 0] reglardless of how we interpret those bit strings
              


;Practice 2.34
;Fill in the following table showing the results of multiplying different 3-bit num- bers, in the style of Figure 2.26:

(def data-2-34
  [[[1 0 0] [1 0 1]]
   [[0 1 0] [1 1 1]]
   [[1 1 0] [1 1 0]]])


(defn solve-2-34 []
  (map
   #(let [s-x (twos-complement (first %1))
          s-y (twos-complement (second %1))
          u-x (binary-to-decimal (first %1))
          u-y (binary-to-decimal (second %1))]
      
       {:s-x              s-x
       :s-y               s-y 
       :s-x*y             (* s-x s-y)
       :s-trunc-x*y-bin   (decimal-to-binary (* s-x s-y) 3)
       :s-trunc-x*y-dec   (twos-complement (decimal-to-binary (* s-x s-y) 3))
       :u-x               u-x
       :u-y               u-y
       :u-x*y             (* u-x u-y)
       :u-trunc-x*y-bin   (decimal-to-binary (* s-x s-y) 3)
       :u-trunc-x*y-dec   (binary-to-decimal (decimal-to-binary (* s-x s-y) 3))})
   data-2-34))

;opperations on unsigned are isomorphic with operations on signed



;Practice Problem 2.45
;Fill in the missing information in the following table
;Fraction, binary, decimal
(def data-2-45
  [[1/8 "0.001" 0.125]
   [3/4 nil nil]
   [25/16 nil nil]
   [nil "10.1011" nil]
   [nil "1.001" nil]
   [nil nil 5.875]
   [nil nil 3.1875]])

(defn fraction-to-decimal [fraction]
 (float fraction))

(defn decimal-to-fraction [decimal]
  (rationalize decimal))

(defn binary-to-decimal [binary]
  (let [split-binary (clj-str/split binary #"\.")]
       (+ (loop [power 0 ;the positive exponent loop
                 sum 0]
            (let [digit  (nth (reverse (first split-binary)) power nil)]
              (if (nil? digit)
                sum
                (recur (+ power 1) (+ sum (* (read-string (str digit)) (Math/pow 2 power)))))))
          (loop [power -1 ;the negative exponent loop
                 sum 0]
            (let [digit (nth (second split-binary) (- (Math/abs power) 1) nil)]
              (if (nil? digit)
                sum
                (recur (- power 1) (+ sum (* (read-string (str digit)) (Math/pow 2 power))))))))))

(defn str-insert
  "Insert c in string s at index i."
  [s c i]
  (str (subs s 0 i) c (subs s i)))

(defn fraction-to-binary [fraction]
  (let [min-pow (int (log2 (denominator fraction)))]
    (reverse (str-insert
              (let [r-str
                    (loop [pow 0 
                           num (int (* (float fraction) (Math/pow 2 min-pow)))
                           rv ""]
                      (if (= num 0)
                        rv
                        (recur (+ pow 1) (int (/ num 2)) (str rv (mod num 2)))))

                    padding
                    (- min-pow (count r-str))]
                (if (> padding 0)
                  (str r-str (apply str (take (+ 1 padding) (repeat "0"))))
                  r-str))
              "."
              min-pow))))

(defn get-number-type [number-array]
  (let [types [:fraction :binary :decimal]]
    (loop [index 0]
      (if (or (not (nil? (get number-array index))) (> (+ index 1) (count number-array)))
        {:type (get types index nil) :val (get number-array index nil)}
        (recur (+ index 1))))))

(defmulti to-fraction (fn [input] (:type input)))

(defmethod to-fraction :fraction [input] (input :val))
(defmethod to-fraction :decimal  [input] (rationalize (input :val)))
(defmethod to-fraction :binary  [input] (rationalize (binary-to-decimal (input :val))))


(defmulti to-binary (fn [input] (:type input)))

(defmethod to-binary :fraction [input] (fraction-to-binary (input :val)))
(defmethod to-binary :decimal  [input] (fraction-to-binary (to-fraction input)))
(defmethod to-binary :binary   [input] (input :val))


(defmulti to-decimal (fn [input] (:type input)))

(defmethod to-decimal :fraction [input] (float (input :val)))
(defmethod to-decimal :decimal  [input] (input :val))
(defmethod to-decimal :binary   [input] (binary-to-decimal (input :val)))

(defn solve-2-45 []
  (map
   #(let [input (get-number-type %1)]
      [(to-fraction input)
       (to-binary   input)
       (to-decimal  input)])
   data-2-45))


￼