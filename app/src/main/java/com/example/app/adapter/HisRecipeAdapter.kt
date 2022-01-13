package com.example.app.adapter

import androidx.fragment.app.findFragment
import androidx.navigation.fragment.NavHostFragment
import com.example.app.fragment.account.AccountViewFragment
import com.example.app.fragment.account.AccountViewFragmentDirections
import com.example.app.model.RecipeModel
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class HisRecipeAdapter(options: FirestoreRecyclerOptions<RecipeModel>) : RecipeAdapter(options) {

    override fun onBindViewHolder(holder: RecipeAdapaterVH, position: Int, model: RecipeModel) {
        super.onBindViewHolder(holder, position, model)
        holder.recipe.setOnClickListener {
            val accountViewFragment = holder.itemView.findFragment<AccountViewFragment>()
            val action = AccountViewFragmentDirections.actionAccountViewFragmentToRecipeFragment(model)
            NavHostFragment.findNavController(accountViewFragment).navigate(action)
        }
    }
}
