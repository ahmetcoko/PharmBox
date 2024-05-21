package com.example.organizemedicine.view

class TrieNode {
    var endOfWord: Boolean = false
    var children: MutableMap<Char, TrieNode> = mutableMapOf()
}