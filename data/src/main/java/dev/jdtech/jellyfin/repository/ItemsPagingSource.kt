package dev.jdtech.jellyfin.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.jdtech.jellyfin.models.FindroidBoxSet
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

class ItemsPagingSource(
    private val jellyfinRepository: JellyfinRepository,
    private val parentId: UUID?,
    private val includeTypes: List<BaseItemKind>?,
    private val recursive: Boolean,
    private val sortBy: SortBy,
    private val sortOrder: SortOrder,
) : PagingSource<Int, FindroidItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FindroidItem> {
        val position = params.key ?: 0

        Timber.d("Retrieving position: $position")

        return try {
            val items =
                jellyfinRepository.getItems(
                    parentId = parentId,
                    includeTypes = includeTypes,
                    recursive = recursive,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                    startIndex = position,
                    limit = params.loadSize,
                )
            val visibleItems = coroutineScope {
                items
                    .map { item ->
                        async { item.takeIf { it !is FindroidBoxSet || it.hasPlayableItems() } }
                    }
                    .awaitAll()
                    .filterNotNull()
            }
            LoadResult.Page(
                data = visibleItems,
                prevKey = if (position == 0) null else position - params.loadSize,
                nextKey = if (items.isEmpty()) null else position + params.loadSize,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    // Box sets can consist solely of items Findroid can't play (e.g. books), which would show up
    // as empty collections
    private suspend fun FindroidBoxSet.hasPlayableItems(): Boolean =
        jellyfinRepository
            .getItems(
                parentId = id,
                includeTypes =
                    listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES, BaseItemKind.EPISODE),
                limit = 1,
            )
            .isNotEmpty()

    override fun getRefreshKey(state: PagingState<Int, FindroidItem>): Int {
        return 0
    }
}
