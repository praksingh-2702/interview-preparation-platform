package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private QuestionRepository questionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (questionRepository.count() == 0) {
            seedAllOneHundredQuestions();
        }
    }

    private void seedAllOneHundredQuestions() {
        List<Question> qList = new ArrayList<>();

        // ==========================================
        // 1. ARRAYS (10 Questions)
        // ==========================================
        qList.add(createQuestion("Two Sum", "Arrays", "EASY", "Find indices of two numbers that add up to a target.", "https://leetcode.com/problems/two-sum/", "Google, Amazon"));
        qList.add(createQuestion("Best Time to Buy and Sell Stock", "Arrays", "EASY", "Maximize profit by choosing a single day to buy and a different day to sell.", "https://leetcode.com/problems/best-time-to-buy-and-sell-stock/", "Google, Microsoft"));
        qList.add(createQuestion("Contains Duplicate", "Arrays", "EASY", "Return true if any value appears at least twice in the array.", "https://leetcode.com/problems/contains-duplicate/", "Apple, Adobe"));
        qList.add(createQuestion("Product of Array Except Self", "Arrays", "MEDIUM", "Return an array where output[i] is equal to product of all elements except nums[i].", "https://leetcode.com/problems/product-of-array-except-self/", "Amazon, Facebook"));
        qList.add(createQuestion("Maximum Subarray", "Arrays", "MEDIUM", "Find the contiguous subarray with the largest sum (Kadane's Algorithm).", "https://leetcode.com/problems/maximum-subarray/", "Google, PhonePe"));
        qList.add(createQuestion("3Sum", "Arrays", "MEDIUM", "Find all unique triplets in the array that sum up to zero.", "https://leetcode.com/problems/3sum/", "Amazon, Microsoft"));
        qList.add(createQuestion("Merge Intervals", "Arrays", "MEDIUM", "Merge all overlapping intervals.", "https://leetcode.com/problems/merge-intervals/", "Google, Razorpay"));
        qList.add(createQuestion("Container With Most Water", "Arrays", "MEDIUM", "Find two lines that together with the x-axis form a container containing the most water.", "https://leetcode.com/problems/container-with-most-water/", "Google, Apple"));
        qList.add(createQuestion("Trapping Rain Water", "Arrays", "HARD", "Compute how much water it can trap after raining.", "https://leetcode.com/problems/trapping-rain-water/", "Google, Amazon"));
        qList.add(createQuestion("Find the Duplicate Number", "Arrays", "MEDIUM", "Find the duplicate number in an array of n+1 integers where each integer is between 1 and n.", "https://leetcode.com/problems/find-the-duplicate-number/", "Microsoft, Adobe"));

        // ==========================================
        // 2. BINARY SEARCH (10 Questions)
        // ==========================================
        qList.add(createQuestion("Binary Search", "Binary Search", "EASY", "Implement standard binary search on a sorted array.", "https://leetcode.com/problems/binary-search/", "Google, Apple"));
        qList.add(createQuestion("Search in Rotated Sorted Array", "Binary Search", "MEDIUM", "Find an element in a rotated sorted array.", "https://leetcode.com/problems/search-in-rotated-sorted-array/", "Google, Microsoft"));
        qList.add(createQuestion("First and Last Position in Sorted Array", "Binary Search", "MEDIUM", "Find starting and ending position of a given target value.", "https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/", "Amazon, Adobe"));
        qList.add(createQuestion("Find Minimum in Rotated Sorted Array", "Binary Search", "MEDIUM", "Find the minimum element in a rotated sorted array.", "https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/", "Amazon, PhonePe"));
        qList.add(createQuestion("Peak Index in a Mountain Array", "Binary Search", "MEDIUM", "Find the peak index in a mountain array.", "https://leetcode.com/problems/peak-index-in-a-mountain-array/", "Google, Facebook"));
        qList.add(createQuestion("Search a 2D Matrix", "Binary Search", "MEDIUM", "Search for a value in an m x n matrix with sorted rows and columns.", "https://leetcode.com/problems/search-a-2d-matrix/", "Microsoft, Razorpay"));
        qList.add(createQuestion("Koko Eating Bananas", "Binary Search", "MEDIUM", "Find the minimum integer speed K to eat all bananas within H hours.", "https://leetcode.com/problems/koko-eating-bananas/", "Google, Amazon"));
        qList.add(createQuestion("Median of Two Sorted Arrays", "Binary Search", "HARD", "Find the median of two sorted arrays in O(log(m+n)) time.", "https://leetcode.com/problems/median-of-two-sorted-arrays/", "Google, Apple"));
        qList.add(createQuestion("Capacity To Ship Packages Within D Days", "Binary Search", "MEDIUM", "Find the least weight capacity of a boat to ship packages within D days.", "https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/", "Amazon, Google"));
        qList.add(createQuestion("Find Peak Element", "Binary Search", "MEDIUM", "Find a peak element that is strictly greater than its neighbors.", "https://leetcode.com/problems/find-peak-element/", "Facebook, Microsoft"));

        // ==========================================
        // 3. BIT MANIPULATION (10 Questions)
        // ==========================================
        qList.add(createQuestion("Number of 1 Bits", "Bit Manipulation", "EASY", "Return the number of '1' bits an unsigned integer has.", "https://leetcode.com/problems/number-of-1-bits/", "Apple, Amazon"));
        qList.add(createQuestion("Counting Bits", "Bit Manipulation", "EASY", "Return an array of the number of 1s for each number from 0 to n.", "https://leetcode.com/problems/counting-bits/", "Microsoft, Adobe"));
        qList.add(createQuestion("Single Number", "Bit Manipulation", "EASY", "Find the element that appears only once in an array where others appear twice.", "https://leetcode.com/problems/single-number/", "Google, Amazon"));
        qList.add(createQuestion("Missing Number", "Bit Manipulation", "EASY", "Find the only number missing from the range [0, n].", "https://leetcode.com/problems/missing-number/", "Apple, PhonePe"));
        qList.add(createQuestion("Reverse Bits", "Bit Manipulation", "EASY", "Reverse bits of a given 32-bit unsigned integer.", "https://leetcode.com/problems/reverse-bits/", "Google, Microsoft"));
        qList.add(createQuestion("Sum of Two Integers", "Bit Manipulation", "MEDIUM", "Sum two integers without using + or - operators.", "https://leetcode.com/problems/sum-of-two-integers/", "Facebook, Amazon"));
        qList.add(createQuestion("Bitwise AND of Numbers Range", "Bit Manipulation", "MEDIUM", "Return the bitwise AND of all numbers in a range inclusive.", "https://leetcode.com/problems/bitwise-and-of-numbers-range/", "Google, Adobe"));
        qList.add(createQuestion("Single Number II", "Bit Manipulation", "MEDIUM", "Find the element that appears once where all others appear three times.", "https://leetcode.com/problems/single-number-ii/", "Google, Razorpay"));
        qList.add(createQuestion("UTF-8 Validation", "Bit Manipulation", "MEDIUM", "Determine whether a given array of integers represents valid UTF-8 data.", "https://leetcode.com/problems/utf-8-validation/", "Google, Amazon"));
        qList.add(createQuestion("Reverse Pairs", "Bit Manipulation", "HARD", "Count important reverse pairs in an array.", "https://leetcode.com/problems/reverse-pairs/", "Google, Microsoft"));

        // ==========================================
        // 4. LINKED LISTS (10 Questions)
        // ==========================================
        qList.add(createQuestion("Reverse a Linked List", "Linked Lists", "EASY", "Invert the sequential pointers of a singly linked list.", "https://leetcode.com/problems/reverse-linked-list/", "Microsoft, PhonePe"));
        qList.add(createQuestion("Linked List Cycle", "Linked Lists", "EASY", "Determine if a linked list contains a cycle loop.", "https://leetcode.com/problems/linked-list-cycle/", "Google, Amazon"));
        qList.add(createQuestion("Merge Two Sorted Lists", "Linked Lists", "EASY", "Merge two sorted linked lists sequentially into one.", "https://leetcode.com/problems/merge-two-sorted-lists/", "Apple, Adobe"));
        qList.add(createQuestion("Remove Nth Node From End of List", "Linked Lists", "MEDIUM", "Remove the nth node from the end of a list and return its head.", "https://leetcode.com/problems/remove-nth-node-from-end-of-list/", "Amazon, Facebook"));
        qList.add(createQuestion("Reorder List", "Linked Lists", "MEDIUM", "Reorder a list to alternate between start and end nodes.", "https://leetcode.com/problems/reorder-list/", "Google, Microsoft"));
        qList.add(createQuestion("Linked List Cycle II", "Linked Lists", "MEDIUM", "Find the exact node where the cycle loop begins.", "https://leetcode.com/problems/linked-list-cycle-ii/", "Google, Razorpay"));
        qList.add(createQuestion("Add Two Numbers", "Linked Lists", "MEDIUM", "Add two numbers represented by linked lists in reverse order.", "https://leetcode.com/problems/add-two-numbers/", "Amazon, Microsoft"));
        qList.add(createQuestion("Merge k Sorted Lists", "Linked Lists", "HARD", "Merge k sorted linked lists into one single sorted list.", "https://leetcode.com/problems/merge-k-sorted-lists/", "Google, Facebook"));
        qList.add(createQuestion("Reverse Nodes in k-Group", "Linked Lists", "HARD", "Reverse nodes of a linked list k at a time.", "https://leetcode.com/problems/reverse-nodes-in-k-group/", "Microsoft, Amazon"));
        qList.add(createQuestion("Copy List with Random Pointer", "Linked Lists", "MEDIUM", "Construct a deep copy of a list containing a random pointer field.", "https://leetcode.com/problems/copy-list-with-random-pointer/", "Google, Adobe"));

        // ==========================================
        // 5. STACKS & QUEUES (10 Questions)
        // ==========================================
        qList.add(createQuestion("Valid Parentheses", "Stacks & Queues", "EASY", "Determine if bracket input strings are closed in correct structural order.", "https://leetcode.com/problems/valid-parentheses/", "Google, Razorpay"));
        qList.add(createQuestion("Min Stack", "Stacks & Queues", "EASY", "Design a stack that supports push, pop, top, and retrieving the minimum element in O(1) time.", "https://leetcode.com/problems/min-stack/", "Amazon, Microsoft"));
        qList.add(createQuestion("Evaluate Reverse Polish Notation", "Stacks & Queues", "MEDIUM", "Evaluate the value of an arithmetic expression in Reverse Polish Notation.", "https://leetcode.com/problems/evaluate-reverse-polish-notation/", "Google, PhonePe"));
        qList.add(createQuestion("Generate Parentheses", "Stacks & Queues", "MEDIUM", "Generate all combinations of well-formed parentheses for n pairs.", "https://leetcode.com/problems/generate-parentheses/", "Amazon, Facebook"));
        qList.add(createQuestion("Daily Temperatures", "Stacks & Queues", "MEDIUM", "Return an array of the number of days you have to wait for a warmer temperature.", "https://leetcode.com/problems/daily-temperatures/", "Google, Apple"));
        qList.add(createQuestion("Car Fleet", "Stacks & Queues", "MEDIUM", "Determine how many car fleets will arrive at the destination destination.", "https://leetcode.com/problems/car-fleet/", "Google, Amazon"));
        qList.add(createQuestion("Largest Rectangle in Histogram", "Stacks & Queues", "HARD", "Find the area of the largest rectangle in a histogram grid.", "https://leetcode.com/problems/largest-rectangle-in-histogram/", "Google, Microsoft"));
        qList.add(createQuestion("Sliding Window Maximum", "Stacks & Queues", "HARD", "Return the max element inside a sliding window running across an array.", "https://leetcode.com/problems/sliding-window-maximum/", "Amazon, Google"));
        qList.add(createQuestion("Implement Queue using Stacks", "Stacks & Queues", "EASY", "Implement a first-in first-out queue using only standard stacks.", "https://leetcode.com/problems/implement-queue-using-stacks/", "Microsoft, Adobe"));
        qList.add(createQuestion("Online Stock Span", "Stacks & Queues", "MEDIUM", "Calculate the span of a stock's price for the current day based on preceding records.", "https://leetcode.com/problems/online-stock-span/", "PhonePe, Razorpay"));

        // ==========================================
        // 6. RECURSION & BACKTRACKING (10 Questions)
        // ==========================================
        qList.add(createQuestion("Subsets", "Recursion & Backtracking", "MEDIUM", "Return all possible subsets (the power set) of a unique integer array.", "https://leetcode.com/problems/subsets/", "Amazon, Facebook"));
        qList.add(createQuestion("Combination Sum", "Recursion & Backtracking", "MEDIUM", "Return a list of all unique combinations where the chosen numbers sum to a target.", "https://leetcode.com/problems/combination-sum/", "Google, Microsoft"));
        qList.add(createQuestion("Permutations", "Recursion & Backtracking", "MEDIUM", "Return all possible permutations of an array of distinct integers.", "https://leetcode.com/problems/permutations/", "Google, Adobe"));
        qList.add(createQuestion("Subsets II", "Recursion & Backtracking", "MEDIUM", "Return all possible subsets from an array containing duplicate elements.", "https://leetcode.com/problems/subsets-ii/", "Amazon, PhonePe"));
        qList.add(createQuestion("Combination Sum II", "Recursion & Backtracking", "MEDIUM", "Find all unique combinations in a collection where numbers sum to a target.", "https://leetcode.com/problems/combination-sum-ii/", "Google, Razorpay"));
        qList.add(createQuestion("Word Search", "Recursion & Backtracking", "MEDIUM", "Determine if a word exists sequentially inside a 2D character board grid.", "https://leetcode.com/problems/word-search/", "Amazon, Apple"));
        qList.add(createQuestion("Palindrome Partitioning", "Recursion & Backtracking", "MEDIUM", "Partition a string such that every substring of the partition is a palindrome.", "https://leetcode.com/problems/palindrome-partitioning/", "Google, Facebook"));
        qList.add(createQuestion("Letter Combinations of a Phone Number", "Recursion & Backtracking", "MEDIUM", "Return all possible letter combinations that a digit sequence string could represent.", "https://leetcode.com/problems/letter-combinations-of-a-phone-number/", "Microsoft, Amazon"));
        qList.add(createQuestion("N-Queens", "Recursion & Backtracking", "HARD", "Place n queens on an n x n chessboard such that no two queens attack each other.", "https://leetcode.com/problems/n-queens/", "Google, Microsoft"));
        qList.add(createQuestion("Sudoku Solver", "Recursion & Backtracking", "HARD", "Write a program to solve a Sudoku puzzle by filling the empty cells.", "https://leetcode.com/problems/sudoku-solver/", "Google, Amazon"));

        // ==========================================
        // 7. GREEDY ALGORITHMS (10 Questions)
        // ==========================================
        qList.add(createQuestion("Jump Game", "Greedy Algorithms", "MEDIUM", "Determine if you can reach the final index given maximum step limits.", "https://leetcode.com/problems/jump-game/", "Google, Apple"));
        qList.add(createQuestion("Jump Game II", "Greedy Algorithms", "MEDIUM", "Find the minimum number of jumps to reach the final index.", "https://leetcode.com/problems/jump-game-ii/", "Google, Amazon"));
        qList.add(createQuestion("Gas Station", "Greedy Algorithms", "MEDIUM", "Find the starting gas station index to travel clockwise around a circuit once.", "https://leetcode.com/problems/gas-station/", "Amazon, Microsoft"));
        qList.add(createQuestion("Hand of Straights", "Greedy Algorithms", "MEDIUM", "Determine if cards can be rearranged into groups of consecutive consecutive numbers.", "https://leetcode.com/problems/hand-of-straights/", "Google, Adobe"));
        qList.add(createQuestion("Merge Triplets to Form Target", "Greedy Algorithms", "MEDIUM", "Determine if a target triplet can be formed by merging operation states.", "https://leetcode.com/problems/merge-triplets-to-form-target/", "Amazon, PhonePe"));
        qList.add(createQuestion("Partition Labels", "Greedy Algorithms", "MEDIUM", "Partition a string into as many parts as possible so that each letter appears in at most one part.", "https://leetcode.com/problems/partition-labels/", "Google, Facebook"));
        qList.add(createQuestion("Valid Parenthesis String", "Greedy Algorithms", "MEDIUM", "Determine if a bracket string containing asterisks wildcards is structurally valid.", "https://leetcode.com/problems/valid-parenthesis-string/", "Google, Razorpay"));
        qList.add(createQuestion("Candy", "Greedy Algorithms", "HARD", "Allocate candies to children based on rating scores minimizing the total count.", "https://leetcode.com/problems/candy/", "Google, Microsoft"));
        qList.add(createQuestion("Assign Cookies", "Greedy Algorithms", "EASY", "Maximize cookie allocation to children based on desire scores.", "https://leetcode.com/problems/assign-cookies/", "Apple, Amazon"));
        qList.add(createQuestion("Task Scheduler", "Greedy Algorithms", "MEDIUM", "Find the least number of CPU intervals needed to complete all tasks given cooling cycles.", "https://leetcode.com/problems/task-scheduler/", "Facebook, Microsoft"));

        // ==========================================
        // 8. TREES (10 Questions)
        // ==========================================
        qList.add(createQuestion("Maximum Depth of Binary Tree", "Trees", "EASY", "Compute the total node path distance along the longest path from root to leaf.", "https://leetcode.com/problems/maximum-depth-of-binary-tree/", "Amazon, Microsoft"));
        qList.add(createQuestion("Invert Binary Tree", "Trees", "EASY", "Invert a binary tree so that left and right children are swapped recursively.", "https://leetcode.com/problems/invert-binary-tree/", "Google, Apple"));
        qList.add(createQuestion("Same Tree", "Trees", "EASY", "Check if two binary trees are structurally identical and have matching node values.", "https://leetcode.com/problems/same-tree/", "Microsoft, Adobe"));
        qList.add(createQuestion("Subtree of Another Tree", "Trees", "EASY", "Determine if a tree contains a subtree matching a separate target structure.", "https://leetcode.com/problems/subtree-of-another-tree/", "Amazon, Facebook"));
        qList.add(createQuestion("Lowest Common Ancestor of a BST", "Trees", "EASY", "Find the lowest common ancestor node of two given nodes in a BST.", "https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-search-tree/", "Google, PhonePe"));
        qList.add(createQuestion("Binary Tree Level Order Traversal", "Trees", "MEDIUM", "Return the level order traversal of its nodes' values as groups.", "https://leetcode.com/problems/binary-tree-level-order-traversal/", "Google, Amazon"));
        qList.add(createQuestion("Binary Tree Right Side View", "Trees", "MEDIUM", "Return the values of the nodes visible when looking from the right side.", "https://leetcode.com/problems/binary-tree-right-side-view/", "Amazon, Facebook"));
        qList.add(createQuestion("Count Good Nodes in Binary Tree", "Trees", "MEDIUM", "Count nodes where value is greater than or equal to max seen from root.", "https://leetcode.com/problems/count-good-nodes-in-binary-tree/", "Microsoft, Razorpay"));
        qList.add(createQuestion("Validate Binary Search Tree", "Trees", "MEDIUM", "Determine if a binary tree is a structurally valid Binary Search Tree.", "https://leetcode.com/problems/validate-binary-search-tree/", "Google, Microsoft"));
        qList.add(createQuestion("Binary Tree Maximum Path Sum", "Trees", "HARD", "Find the maximum path sum between any two nodes in a tree sequence.", "https://leetcode.com/problems/binary-tree-maximum-path-sum/", "Google, Amazon"));

        // ==========================================
        // 9. GRAPHS (10 Questions)
        // ==========================================
        qList.add(createQuestion("Number of Islands", "Graphs", "MEDIUM", "Traverse a 2D grid mapping clusters of '1's (land) surrounded by water.", "https://leetcode.com/problems/number-of-islands/", "Google, Razorpay"));
        qList.add(createQuestion("Clone Graph", "Graphs", "MEDIUM", "Construct a deep copy clone of a connected undirected graph structure.", "https://leetcode.com/problems/clone-graph/", "Google, Facebook"));
        qList.add(createQuestion("Max Area of Island", "Graphs", "MEDIUM", "Find the maximum area among islands mapped inside a 2D grid matrix.", "https://leetcode.com/problems/max-area-of-island/", "Amazon, Microsoft"));
        qList.add(createQuestion("Pacific Atlantic Water Flow", "Graphs", "MEDIUM", "Find grid coordinate nodes that can shed rainwater to both oceans.", "https://leetcode.com/problems/pacific-atlantic-water-flow/", "Google, Amazon"));
        qList.add(createQuestion("Surrounded Regions", "Graphs", "MEDIUM", "Capture surrounded regions by flipping all unescaped inner boundary 'O's.", "https://leetcode.com/problems/surrounded-regions/", "Microsoft, Adobe"));
        qList.add(createQuestion("Rotting Oranges", "Graphs", "MEDIUM", "Find the minimum minutes elapsed until no fresh orange orange remains.", "https://leetcode.com/problems/rotting-oranges/", "Amazon, PhonePe"));
        qList.add(createQuestion("Course Schedule", "Graphs", "MEDIUM", "Determine if it's possible to finish all courses given prerequisite dependencies.", "https://leetcode.com/problems/course-schedule/", "Google, Amazon"));
        qList.add(createQuestion("Course Schedule II", "Graphs", "MEDIUM", "Return the exact ordering order of courses required to complete study streams.", "https://leetcode.com/problems/course-schedule-ii/", "Google, Facebook"));
        qList.add(createQuestion("Redundant Connection", "Graphs", "MEDIUM", "Find an edge that can be removed so the resulting graph is a cycle-free tree.", "https://leetcode.com/problems/redundant-connection/", "Amazon, Microsoft"));
        qList.add(createQuestion("Word Ladder", "Graphs", "HARD", "Find the length of shortest transformation sequence converting start word to end word.", "https://leetcode.com/problems/word-ladder/", "Google, Amazon"));

        // ==========================================
        // 10. DYNAMIC PROGRAMMING (10 Questions)
        // ==========================================
        qList.add(createQuestion("Longest Common Subsequence", "Dynamic Programming", "MEDIUM", "Compute the longest length subsequential string match present between two inputs.", "https://leetcode.com/problems/longest-common-subsequence/", "PhonePe, Adobe"));
        qList.add(createQuestion("Climbing Stairs", "Dynamic Programming", "EASY", "Find how many distinct ways you can climb to the top of n steps.", "https://leetcode.com/problems/climbing-stairs/", "Google, Apple"));
        qList.add(createQuestion("Min Cost Climbing Stairs", "Dynamic Programming", "EASY", "Find the minimum cost to reach the top floor from step weights.", "https://leetcode.com/problems/min-cost-climbing-stairs/", "Amazon, Microsoft"));
        qList.add(createQuestion("House Robber", "Dynamic Programming", "MEDIUM", "Maximize stolen treasure from houses without alerting adjacent alarms.", "https://leetcode.com/problems/house-robber/", "Google, Facebook"));
        qList.add(createQuestion("House Robber II", "Dynamic Programming", "MEDIUM", "Maximize stolen wealth from houses arranged in a circular perimeter layout.", "https://leetcode.com/problems/house-robber-ii/", "Microsoft, Amazon"));
        qList.add(createQuestion("Longest Palindromic Substring", "Dynamic Programming", "MEDIUM", "Find the longest palindromic substring inside a string input sequence.", "https://leetcode.com/problems/longest-palindromic-substring/", "Google, Amazon"));
        qList.add(createQuestion("Decode Ways", "Dynamic Programming", "MEDIUM", "Find the total number of ways to decode an encoded message string.", "https://leetcode.com/problems/decode-ways/", "Amazon, Adobe"));
        qList.add(createQuestion("Coin Change", "Dynamic Programming", "MEDIUM", "Compute the fewest number of coins needed to make up a specific target sum.", "https://leetcode.com/problems/coin-change/", "Google, Razorpay"));
        qList.add(createQuestion("Maximum Product Subarray", "Dynamic Programming", "MEDIUM", "Find a contiguous subarray that has the largest multiplication product value.", "https://leetcode.com/problems/maximum-product-subarray/", "Amazon, Facebook"));
        qList.add(createQuestion("Longest Increasing Subsequence", "Dynamic Programming", "MEDIUM", "Find the length of the longest strictly increasing subsequence loop.", "https://leetcode.com/problems/longest-increasing-subsequence/", "Google, Microsoft"));

        questionRepository.saveAll(qList);
        System.out.println(">> Database successfully seeded with 10 questions for all 10 topics (Total: 100 questions)!");
    }

    private Question createQuestion(String title, String category, String difficulty, String description, String problemUrl, String companiesStr) {
        Question q = new Question();
        q.setTitle(title);
        q.setCategory(category);
        q.setDifficulty(difficulty);
        q.setDescription(description);
        q.setProblemUrl(problemUrl);
        
        String[] comps = companiesStr.split(", ");
        q.setCompanies(new HashSet<>(Arrays.asList(comps)));
        
        return q;
    }
}