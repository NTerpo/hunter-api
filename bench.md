# Performance Benchmarks

## 2015/04/12

    hunter-api.data> (bench (search "consumer"))

    Evaluation count : 9480 in 60 samples of 158 calls.
             Execution time mean : 6.966718 ms
    Execution time std-deviation : 1.158336 ms
    Execution time lower quantile : 6.020424 ms ( 2.5%)
    Execution time upper quantile : 10.147684 ms (97.5%)
                   Overhead used : 2.154947 ns

    Found 8 outliers in 60 samples (13.3333 %)
	    low-severe	 5 (8.3333 %)
	    low-mild	 3 (5.0000 %)
    Variance from outliers : 87.5983 % Variance is severely inflated by outliers
nil
