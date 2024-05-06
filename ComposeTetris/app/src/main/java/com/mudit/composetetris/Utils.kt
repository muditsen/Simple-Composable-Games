package com.mudit.composetetris


val Int.row :Int
    get() {
        return this / TetrisViewModel.COLUMNS
    }

val Int.column :Int
    get() {
        return this % TetrisViewModel.COLUMNS
    }