package com.example.app.adapter

import androidx.fragment.app.findFragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.example.app.fragment.home.HomeFragment
import com.example.app.fragment.home.HomeFragmentDirections
import com.example.app.model.RecipeModel
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class HomeRecipeAdapter(options: FirestoreRecyclerOptions<RecipeModel>) : RecipeAdapter(options) {

    override fun onBindViewHolder(holder: RecipeAdapaterVH, position: Int, model: RecipeModel) {
        super.onBindViewHolder(holder, position, model)
        holder.recipe.setOnClickListener {
            val homeFragment = holder.itemView.findFragment<HomeFragment>()
            val action: NavDirections = HomeFragmentDirections.actionNavigationHomeToRecipeFragment(model)
            NavHostFragment.findNavController(homeFragment).navigate(action)
        }
    }
}