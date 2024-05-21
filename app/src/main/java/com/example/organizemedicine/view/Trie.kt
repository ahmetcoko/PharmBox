package com.example.organizemedicine.view

class Trie {
    var root: TrieNode = TrieNode()

    fun insert(word: String) {
        var current: TrieNode = root
        for (l in word) {
            current = current.children.getOrPut(l) { TrieNode() }
        }
        current.endOfWord = true
    }

    fun search(word: String): Boolean {
        var current: TrieNode = root
        for (l in word) {
            current = current.children[l] ?: return false
        }
        return current.endOfWord
    }

    fun startsWith(prefix: String): Boolean {
        var current: TrieNode = root
        for (l in prefix) {
            current = current.children[l] ?: return false
        }
        return true
    }

    fun wordsWithPrefix(prefix: String, limit: Int = 100): List<String> {
        val results = mutableListOf<String>()
        var current: TrieNode = root
        for (l in prefix) {
            current = current.children[l] ?: return emptyList()
        }
        findAllWords(current, prefix, results, limit)
        return results
    }

    private fun findAllWords(node: TrieNode, prefix: String, results: MutableList<String>, limit: Int) {
        if (results.size >= limit) {
            return
        }
        if (node.endOfWord) {
            results.add(prefix)
        }
        for ((char, childNode) in node.children) {
            findAllWords(childNode, prefix + char, results, limit)
        }
    }
}